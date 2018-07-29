package com.github.j5ik2o.reactive.memcached

import java.time.ZonedDateTime
import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{ LogSource, Logging, LoggingAdapter }
import akka.stream._
import akka.stream.scaladsl.{
  Flow,
  GraphDSL,
  Keep,
  RestartFlow,
  Sink,
  Source,
  SourceQueueWithComplete,
  Tcp,
  Unzip,
  Zip
}
import akka.util.ByteString
import cats.implicits._
import com.github.j5ik2o.reactive.memcached.command.{ CommandRequest, CommandResponse }
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.{ Future, Promise }

object MemcachedConnection {

  implicit val logSource: LogSource[MemcachedConnection] = new LogSource[MemcachedConnection] {
    override def genString(o: MemcachedConnection): String =
      s"connection:${o.id}:${o.peerConfig.map(_.remoteAddress.toString).getOrElse("")}"
    override def getClazz(o: MemcachedConnection): Class[_] = o.getClass
  }

  final val DEFAULT_DECIDER: Supervision.Decider = {
    case _: StreamTcpException => Supervision.Restart
    case _                     => Supervision.Stop
  }

  def apply(peerConfig: PeerConfig,
            supervisionDecider: Option[Supervision.Decider])(implicit system: ActorSystem): MemcachedConnection =
    new MemcachedConnectionImpl(peerConfig, supervisionDecider)

}

trait MemcachedConnection {
  def id: UUID
  def shutdown(): Unit
  def peerConfig: Option[PeerConfig]
  def send[C <: CommandRequest](cmd: C): Task[cmd.Response]

  def toFlow[C <: CommandRequest](
      parallelism: Int = 1
  )(implicit scheduler: Scheduler): Flow[C, C#Response, NotUsed] =
    Flow[C].mapAsync(parallelism) { cmd =>
      send(cmd).runAsync
    }
}

private[memcached] class MemcachedConnectionImpl(_peerConfig: PeerConfig,
                                                 supervisionDecider: Option[Supervision.Decider])(
    implicit system: ActorSystem
) extends MemcachedConnection {

  override def id: UUID = UUID.randomUUID()

  val peerConfig = Some(_peerConfig)
  import _peerConfig._
  import _peerConfig.backoffConfig._

  private val log: LoggingAdapter = Logging(system, this)

  private implicit val mat: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(
      supervisionDecider.getOrElse(MemcachedConnection.DEFAULT_DECIDER)
    )
  )

  protected val tcpFlow: Flow[ByteString, ByteString, NotUsed] =
    RestartFlow.withBackoff(minBackoff, maxBackoff, randomFactor, maxRestarts) { () =>
      Tcp()
        .outgoingConnection(remoteAddress, localAddress, options, halfClose, connectTimeout, idleTimeout)
    }

  protected val connectionFlow: Flow[RequestContext, ResponseContext, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val requestFlow = b.add(
        Flow[RequestContext]
          .map { rc =>
            log.debug(s"request = [{}]", rc.commandRequestString)
            (ByteString.fromString(rc.commandRequest.asString + "\r\n"), rc)
          }
      )
      val responseFlow = b.add(Flow[(ByteString, RequestContext)].map {
        case (byteString, requestContext) =>
          log.debug(s"response = [{}]", byteString.utf8String)
          ResponseContext(byteString, requestContext)
      })
      val unzip = b.add(Unzip[ByteString, RequestContext]())
      val zip   = b.add(Zip[ByteString, RequestContext]())
      requestFlow.out ~> unzip.in
      unzip.out0 ~> tcpFlow ~> zip.in0
      unzip.out1 ~> zip.in1
      zip.out ~> responseFlow.in
      FlowShape(requestFlow.in, responseFlow.out)
    })

  protected val (requestQueue: SourceQueueWithComplete[RequestContext], killSwitch: UniqueKillSwitch) = Source
    .queue[RequestContext](requestBufferSize, overflowStrategy)
    .via(connectionFlow)
    .map { responseContext =>
      log.debug(s"req_id = {}, command = {}: parse",
                responseContext.commandRequestId,
                responseContext.commandRequestString)
      val result = responseContext.parseResponse
      responseContext.completePromise(result.toTry)
    }
    .viaMat(KillSwitches.single)(Keep.both)
    .toMat(Sink.ignore)(Keep.left)
    .run()

  override def shutdown(): Unit = killSwitch.shutdown()

  override def send[C <: CommandRequest](cmd: C): Task[cmd.Response] = Task.deferFutureAction { implicit ec =>
    val promise = Promise[CommandResponse]()
    requestQueue
      .offer(RequestContext(cmd, promise, ZonedDateTime.now()))
      .flatMap {
        case QueueOfferResult.Enqueued =>
          promise.future.map(_.asInstanceOf[cmd.Response])
        case QueueOfferResult.Failure(t) =>
          Future.failed(BufferOfferException("Failed to send request", Some(t)))
        case QueueOfferResult.Dropped =>
          Future.failed(
            BufferOfferException(
              s"Failed to send request, the queue buffer was full."
            )
          )
        case QueueOfferResult.QueueClosed =>
          Future.failed(BufferOfferException("Failed to send request, the queue was closed"))
      }
  }

}
