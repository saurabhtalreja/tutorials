package com.baeldung.kovert

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import nl.komponents.kovenant.functional.bind
import org.kodein.di.Kodein
import org.kodein.di.conf.global
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uy.klutter.config.typesafe.ClassResourceConfig
import uy.klutter.config.typesafe.ReferenceConfig
import uy.klutter.config.typesafe.kodein.importConfig
import uy.klutter.config.typesafe.loadConfig
import uy.klutter.vertx.kodein.KodeinVertx
import uy.kohesive.kovert.core.HttpErrorCode
import uy.kohesive.kovert.core.HttpErrorCodeWithBody
import uy.kohesive.kovert.core.HttpErrorForbidden
import uy.kohesive.kovert.vertx.bindController
import uy.kohesive.kovert.vertx.boot.KodeinKovertVertx
import uy.kohesive.kovert.vertx.boot.KovertVerticle
import uy.kohesive.kovert.vertx.boot.KovertVerticleModule
import uy.kohesive.kovert.vertx.boot.KovertVertx


class ErrorServer {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(ErrorServer::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            ErrorServer().start()
        }
    }

    class ErrorController {
        fun RoutingContext.getForbidden() {
            throw HttpErrorForbidden()
        }
        fun RoutingContext.getError() {
            throw HttpErrorCode("Something went wrong", 590)
        }
        fun RoutingContext.getErrorbody() {
            throw HttpErrorCodeWithBody("Something went wrong", 591, "Body here")
        }
    }

    fun start() {
        Kodein.global.addImport(Kodein.Module {
            importConfig(loadConfig(ClassResourceConfig("/kovert.conf", ErrorServer::class.java), ReferenceConfig())) {
                import("kovert.vertx", KodeinKovertVertx.configModule)
                import("kovert.server", KovertVerticleModule.configModule)
            }

            // includes jackson ObjectMapper to match compatibility with Vertx, app logging via Vertx facade to Slf4j
            import(KodeinVertx.moduleWithLoggingToSlf4j)
            // Kovert boot
            import(KodeinKovertVertx.module)
            import(KovertVerticleModule.module)
        })

        val initControllers = fun Router.() {
            bindController(ErrorController(), "api")
        }

        // startup asynchronously...
        KovertVertx.start() bind { vertx ->
            KovertVerticle.deploy(vertx, routerInit = initControllers)
        } success { deploymentId ->
            LOG.warn("Deployment complete.")
        } fail { error ->
            LOG.error("Deployment failed!", error)
        }

    }
}