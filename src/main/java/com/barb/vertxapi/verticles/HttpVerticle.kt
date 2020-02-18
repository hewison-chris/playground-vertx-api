package com.barb.vertxapi.verticles

import com.barb.vertxapi.utils.Consts
import com.barb.vertxapi.web.registerHandler
import com.barb.vertxapi.web.registerSuspendableHandler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.http.closeAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult

class HttpVerticle(val port: Int) : CoroutineVerticle() {
  override suspend fun start() {
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.route("/").registerHandler { getIndex(it) }
    router.route("/").registerSuspendableHandler { getWhiskiesHandler(it) }
    router["/api/whiskies"].registerSuspendableHandler { rc: RoutingContext -> getWhiskiesHandler(rc) }
    router["/api/whiskies/:id"].registerSuspendableHandler { rc: RoutingContext -> getWhiskiesHandler(rc) }
    router.post("/api/whiskies").registerSuspendableHandler { rc: RoutingContext -> addWhiskyHandler(rc) }
    router.delete("/api/whiskies/:id").registerSuspendableHandler { rc: RoutingContext -> deleteWhiskyHandler(rc) }

    vertx.createHttpServer().apply {
      try {
        requestHandler(router).listenAwait(port)
      } catch (e: Exception) {
        closeAwait()
        throw e
      }
    }
  }

  private fun getIndex(routingContext: RoutingContext) {
    routingContext.response()
        .putHeader("content-type", "text/html")
        .end("<h1>Hello Sexy coroutines, I love you :-)</h1>")
  }

  private fun getWhiskiesHandler(rc: RoutingContext) {
    val request = rc.request()
    val response = rc.response()
    val id = request.getParam("id")
    response
        .putHeader("content-type", "application/json; charset=utf-8")
//    if (id == null) {
//      response.end(Json.encodePrettily(products.values()));
//    } else {
//      final Whisky whisky = products.get(Integer.valueOf(id));
//      if (whisky == null) {
//        response.setStatusCode(404).end();
//      } else {
//        response.end(Json.encodePrettily(whisky));
//      }
//    }
    response.end()
  }

  private suspend fun addWhiskyHandler(rc: RoutingContext) {
    val response = rc.response()
    response.putHeader("content-type", "application/json; charset=utf-8")
    awaitResult<Message<String>> {
      vertx.eventBus().send(
          Consts.EVENT_BUS_DATA_API,
          rc.bodyAsJson,
          DeliveryOptions().addHeader("action", "set"))
    }
    response.setStatusCode(201).end()
  }

  private fun deleteWhiskyHandler(rc: RoutingContext) {
    val request = rc.request()
    val response = rc.response()
    val id = request.getParam("id")
    if (id == null) {
      response.setStatusCode(400).end()
    } else {
      response.setStatusCode(204).end()
    }
  }
}