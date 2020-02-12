package com.barb.vertxapi.verticles

import com.barb.vertxapi.utils.Consts
import com.barb.vertxapi.utils.Consts.EVENT_BUS_DATA_API
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.net.SocketAddress
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.redis.client.connectAwait
import io.vertx.redis.client.Redis.createClient
import io.vertx.redis.client.RedisOptions
import io.vertx.redis.client.impl.RedisClient
import java.util.*
import kotlin.collections.ArrayList

class DataVerticle(val port: Int, val host: String, val db: String) : CoroutineVerticle() {
  lateinit var redisClient: RedisClient

  override suspend fun start() {
    redisClient = createClient(
        vertx,
        RedisOptions()
            .addEndpoint(SocketAddress.inetSocketAddress(port, host))
            .setSelect(config.getInteger(Consts.REDIS_DB, 0))
    ).connectAwait() as RedisClient
    vertx.eventBus().consumer<String>(EVENT_BUS_DATA_API) { handleApiMessage(it) }
  }

  private fun handleApiMessage(message: Message<String>) {
    val redisKey = "whisky:" + RANDOM.nextInt()
//    when (message.headers()["action"]) {
//      "set" -> {
//        redisClient.send(redisKey, message.body(), )
//        val body = message.body().encode()
//        val p = ArrayList<String>()
//        p.add(redisKey)
//        p.add(body)
//        redisApi.rxSet(p)
//            .subscribe(
//                { success: Response? -> message.reply("") }
//            ) { error: Throwable -> message.fail(500, error.message) }
//      }
//      "get" -> redisApi!!.rxGet(redisKey)
//          .subscribe(
//              { success: Response -> message.reply(success.toString()) }
//          ) { error: Throwable -> message.fail(500, error.message) }
//      else -> {
//      }
//    }
  }

  companion object {
    private val RANDOM = Random()
  }
}