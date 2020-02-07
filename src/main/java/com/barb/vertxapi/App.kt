package com.barb.vertxapi

import com.barb.vertxapi.verticles.DataVerticle
import com.barb.vertxapi.verticles.HttpVerticle
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.config.ConfigRetriever
import io.vertx.reactivex.core.Vertx

object App {
  @JvmStatic
  fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val fileStore = ConfigStoreOptions()
        .setType("file")
        .setFormat("properties")
        .setConfig(JsonObject().put("path", "application.properties"))
    val envStore = ConfigStoreOptions().setType("env")
    val retrieverOptions = ConfigRetrieverOptions()
    retrieverOptions.addStore(fileStore).addStore(envStore)
    val retriever = ConfigRetriever.create(vertx, retrieverOptions)
    retriever.rxGetConfig()
        .map { config: JsonObject? -> DeploymentOptions().setConfig(config) }
        .flatMap { options: DeploymentOptions? -> vertx.rxDeployVerticle(DataVerticle::class.java.name, options).map { any: String? -> options } }
        .flatMap { options: DeploymentOptions? -> vertx.rxDeployVerticle(HttpVerticle::class.java.name, options).map { any: String? -> options } }
        .subscribe(
            { deployId: DeploymentOptions? -> println("App started successfully!") }
        ) { error: Throwable ->
          error.printStackTrace()
          System.exit(1)
        }
  }
}