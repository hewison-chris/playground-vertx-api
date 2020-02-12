package com.barb.vertxapi.web

import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * An extension method for simplifying Vert.x Web router handlers
 */
suspend fun Route.registerSuspendableHandler(fn: suspend (RoutingContext) -> Unit): Route =
    handler { ctx ->
      GlobalScope.launch(ctx.vertx().dispatcher()) {
        try {
          fn(ctx)
        } catch (e: Exception) {
          ctx.fail(e)
        }
      }
    }

/**
 * An extension method for simplifying Vert.x Web router handlers
 */
fun Route.registerHandler(fn: (RoutingContext) -> Unit): Route =
    handler { ctx ->
      try {
        fn(ctx)
        if (!ctx.response().ended()) {
          ctx.next()
        }
      } catch (e: Exception) {
        ctx.fail(e)
      }
    }

