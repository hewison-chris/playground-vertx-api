package com.barb.vertxapi.verticles

import com.barb.vertxapi.utils.Consts.EVENT_BUS_DATA_API
import com.barb.vertxapi.vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.net.SocketAddress
import io.vertx.kotlin.core.eventbus.replyAndRequestAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.redis.client.connectAwait
import io.vertx.kotlin.redis.client.sendAwait
import io.vertx.redis.client.Command
import io.vertx.redis.client.Redis.createClient
import io.vertx.redis.client.RedisOptions
import io.vertx.redis.client.Request
import io.vertx.redis.client.impl.RedisClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger { }
/**
 * An extension method for simplifying Vert.x Redis handlers
 */
suspend fun <T> MessageConsumer<T>.registerSuspendableHandler(fn: suspend (Message<T>) -> Unit): MessageConsumer<T> =
    handler { msg ->
      GlobalScope.launch(vertx.dispatcher()) {
        try {
          fn(msg)
        } catch (e: Exception) {
          msg.fail(-1, e.message).also {
            logger.error { e.localizedMessage }
          }
        }
      }
    }

class DataVerticle(val port: Int, val host: String, val db: Int) : CoroutineVerticle() {
  lateinit var redisClient: RedisClient

  override suspend fun start() {
    redisClient = createClient(
        vertx,
        RedisOptions()
            .addEndpoint(SocketAddress.inetSocketAddress(port,host))
            .setSelect(db)
    ).connectAwait() as RedisClient
    vertx.eventBus().consumer<String>(EVENT_BUS_DATA_API)
        .registerSuspendableHandler<String> { handleApiMessage(it) }
  }

  private suspend fun handleApiMessage(message: Message<String>) =
      with(redisClient) {
        when (val action = message.headers()["action"]) {
          "set" -> sendAwait(Request.cmd(Command.APPEND).arg("whisky:" + RANDOM.nextInt()).arg(message.body()))
          "get" -> message.reply(sendAwait(Request.cmd(Command.GET).arg(message.body().toLong())))
          else -> throw Exception("Invalid Redis message action:'$action'")
        }
      }

  companion object {
    private val RANDOM = Random()
  }
}