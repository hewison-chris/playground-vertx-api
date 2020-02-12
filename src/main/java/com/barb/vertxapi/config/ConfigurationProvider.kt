package com.barb.vertxapi.config

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.runBlocking

object ConfigurationProvider {

  fun getConfiguration(vertx: Vertx): JsonObject = runBlocking {

    val fileStore = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "application.properties"))

    val envStore = ConfigStoreOptions()
      .setType("env")

    val retriever = ConfigRetriever.create(
      vertx,
      ConfigRetrieverOptions()
        // The order of registering stores is important, the one registered later overrides the ones registered earlier.
        .addStore(fileStore)
        .addStore(envStore)
    )

    awaitResult<JsonObject> {
      retriever.getConfig(it)
    }
  }
}