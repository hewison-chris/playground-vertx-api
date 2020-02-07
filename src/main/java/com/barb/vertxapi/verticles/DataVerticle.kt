package com.barb.vertxapi.verticles

import com.barb.vertxapi.utils.Consts
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.net.SocketAddress
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.redis.client.Redis
import io.vertx.reactivex.redis.client.RedisAPI
import io.vertx.reactivex.redis.client.Response
import io.vertx.redis.client.RedisOptions
import java.util.*

private const val MAX_RECONNECT_RETRIES = 16

class DataVerticle : AbstractVerticle() {
  private var redisClient: Redis? = null
  private var redisApi: RedisAPI? = null
  override fun start(promise: Promise<Void>) {
    initRedisClient(Handler { event: AsyncResult<Redis?> ->
      if (event.succeeded()) {
        redisApi = RedisAPI.api(redisClient)
        println("Data verticle is deployed successfully")
        promise.complete()
      } else {
        println("Data verticle could not be deployed.")
        promise.fail(event.cause())
      }
    })
    vertx.eventBus().consumer(Consts.EVENT_BUS_DATA_API) { message: Message<JsonObject> -> handleApiMessage(message) }
  }

  private fun handleApiMessage(message: Message<JsonObject>) {
    val action = message.headers()["action"]
    val redisKey = "whisky:" + RANDOM.nextInt()
    when (action) {
      "set" -> {
        val body = message.body().encode()
        val p = ArrayList<String>()
        p.add(redisKey)
        p.add(body)
        redisApi!!.rxSet(p)
            .subscribe(
                { success: Response? -> message.reply("") }
            ) { error: Throwable -> message.fail(500, error.message) }
      }
      "get" -> redisApi!!.rxGet(redisKey)
          .subscribe(
              { success: Response -> message.reply(success.toString()) }
          ) { error: Throwable -> message.fail(500, error.message) }
      else -> {
      }
    }
  }

  private fun initRedisClient(handler: Handler<AsyncResult<Redis?>>) {
    val host = config().getString(Consts.REDIS_HOST)
    val port = config().getInteger(Consts.REDIS_PORT)
    val options = RedisOptions()
        .addEndpoint(SocketAddress.inetSocketAddress(port, host))
        .setSelect(config().getInteger(Consts.REDIS_DB, 0))
    Redis.createClient(vertx, options)
        .rxConnect()
        .subscribe(
            { client: Redis? ->
              redisClient = client
              redisClient!!.exceptionHandler { e: Throwable? -> attemptReconnect(0) }
              handler.handle(Future.succeededFuture())
            }
        ) { error: Throwable ->
          error.printStackTrace()
          handler.handle(Future.failedFuture(error.cause))
        }
  }

  private fun attemptReconnect(retry: Int) {
    if (retry < MAX_RECONNECT_RETRIES) { // we should stop now, as there's nothing we can do.
      println("Redis max reconnect attempt is reached. `$MAX_RECONNECT_RETRIES` times.")
      return
    }
    println("Redis, trying to reconnect... Attempt $retry")
    vertx.setTimer(250) { timer: Long? ->
      initRedisClient(Handler { onReconnect: AsyncResult<Redis?> ->
        if (onReconnect.failed()) {
          attemptReconnect(retry + 1)
        }
      })
    }
  }

  companion object {
    private val RANDOM = Random()
  }
}