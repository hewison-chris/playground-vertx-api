package com.barb.vertxapi

import com.barb.vertxapi.config.ConfigurationProvider
import com.barb.vertxapi.utils.Consts
import com.barb.vertxapi.utils.Consts.APPLICATION_PORT
import com.barb.vertxapi.verticles.DataVerticle
import com.barb.vertxapi.verticles.HttpVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.closeAwait
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

val vertx by lazy {
  Vertx.vertx(VertxOptions()
      .setWorkerPoolSize(1)
      .setInternalBlockingPoolSize(1)
      .setMaxEventLoopExecuteTimeUnit(TimeUnit.MILLISECONDS)
      .setMaxEventLoopExecuteTime(TimeUnit.SECONDS.toMillis(2))
  )
}

val config: JsonObject by lazy {
  Vertx.vertx(
      VertxOptions()
          .setWorkerPoolSize(1)
          .setInternalBlockingPoolSize(1)
  )
      .run {
        val cfg = ConfigurationProvider.getConfiguration(this)
        runBlocking { closeAwait() }
        cfg
      }
}

suspend fun main() {
  listOf(
      HttpVerticle(config.getInteger(APPLICATION_PORT)),
      DataVerticle(config.getInteger(Consts.REDIS_PORT), config.getString(Consts.REDIS_HOST))
  ).map {
    awaitResult<String> { handler ->
      vertx.deployVerticle(
          it,
          DeploymentOptions()
              .setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS)
              .setMaxWorkerExecuteTime(TimeUnit.SECONDS.toMillis(60)),
          handler)
    }
  }
}