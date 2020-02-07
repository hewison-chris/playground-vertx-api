package com.barb.vertxapi.verticles

import com.barb.vertxapi.api.WhiskyRequest
import com.barb.vertxapi.utils.Consts
import io.reactivex.Single
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.core.http.HttpServer
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.handler.BodyHandler

class HttpVerticle : AbstractVerticle() {
  override fun start(promise: Promise<Void>) {
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.route("/").handler { rc: RoutingContext -> indexHandler(rc) }
    router["/api/whiskies"].handler { rc: RoutingContext -> getWhiskiesHandler(rc) }
    router["/api/whiskies/:id"].handler { rc: RoutingContext -> getWhiskiesHandler(rc) }
    router.post("/api/whiskies").handler { rc: RoutingContext -> addWhiskyHandler(rc) }
    router.delete("/api/whiskies/:id").handler { rc: RoutingContext -> deleteWhiskyHandler(rc) }
    val applicationPort = config().getInteger(Consts.APPLICATION_PORT, 8080)
    vertx.createHttpServer()
        .requestHandler(router)
        .rxListen(applicationPort)
        .subscribe(
            { success: HttpServer? -> promise.complete() }
        ) { error: Throwable -> promise.fail(error.cause) }
  }

  private fun indexHandler(rc: RoutingContext) {
    val response = rc.response()
    response
        .putHeader("content-type", "text/html")
        .end("<h1>Hello from my first Vert.x 3 application</h1>")
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

  private fun addWhiskyHandler(rc: RoutingContext) {
    val response = rc.response()
    response
        .putHeader("content-type", "application/json; charset=utf-8")
    Single.just(rc.bodyAsJson)
        .map { body: JsonObject -> body.mapTo(WhiskyRequest::class.java) }
        .flatMap { whiskyRequest: WhiskyRequest? ->
          val options = DeliveryOptions().addHeader("action", "set")
          vertx.eventBus().rxRequest<String?>(Consts.EVENT_BUS_DATA_API, JsonObject.mapFrom(whiskyRequest), options)
        }
        .subscribe(
            { responseItem: Message<String?>? -> response.setStatusCode(201).end() }
        ) { error: Throwable -> response.setStatusCode(500).end(error.message) }
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