package com.barb.vertxapi.verticles

import com.barb.vertxapi.utils.Consts.EVENT_BUS_DATA_API
import io.vertx.core.eventbus.Message
import io.vertx.core.net.SocketAddress
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.redis.client.connectAwait
import io.vertx.kotlin.redis.client.sendAwait
import io.vertx.redis.client.Command
import io.vertx.redis.client.Redis.createClient
import io.vertx.redis.client.RedisClientType
import io.vertx.redis.client.RedisOptions
import io.vertx.redis.client.Request
import io.vertx.redis.client.impl.RedisClient
import kotlinx.coroutines.async
import java.util.*

class DataVerticle(val port: Int, val host: String, val db: Int) : CoroutineVerticle() {
  lateinit var redisClient: RedisClient

  override suspend fun start() {
    redisClient = createClient(
        vertx,
        RedisOptions()
            .addEndpoint(SocketAddress.inetSocketAddress(port, host))
            .setSelect(db)
    ).connectAwait() as RedisClient
    vertx.eventBus().consumer<String>(EVENT_BUS_DATA_API) { handleApiMessage(it) }
  }

  private fun handleApiMessage(message: Message<String>) {
    val redisKey = "whisky:" + RANDOM.nextInt()
    when (message.headers()["action"]) {
      "set" -> async { setCommand(redisKey, message.body()) }
//      "get" ->
    }
  }

  private suspend fun setCommand(key: String, value: String) = redisClient.sendAwait(
      redisClient.send { Request.cmd(Command.APPEND).arg(key).arg(value)) }
  )

  companion object {
    private val RANDOM = Random()
  }
}