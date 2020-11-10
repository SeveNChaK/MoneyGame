package ru.gdcn.game.money

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import ru.gdcn.game.money.RuntimeStorage.changePaperCost
import ru.gdcn.game.money.json.model.Action
import ru.gdcn.game.money.json.model.PaperCost
import ru.gdcn.game.money.json.model.User
import ru.gdcn.game.money.library.UserInfo
import java.time.*
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson {
        }
    }

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                launch {
                    val gson = Gson()
                    for (con in RuntimeStorage.getConnections()) {
                        con.outgoing.send(Frame.Text(gson.toJson(PaperCost(changePaperCost()))))
                    }
                }
            }
        }, 100, 100)

        webSocket("/test") {
            println("New connection.")
            RuntimeStorage.addConnection(this)
            try {
                var isConnected = false
                lateinit var userInfo: UserInfo
                for (frame in incoming){
                    val text = (frame as Frame.Text).readText()
                    val gson = Gson()
                    if (!isConnected) {
                        val connectionUser = gson.fromJson(text, User::class.java)
                        userInfo = RuntimeStorage.getOrCreateUser(connectionUser.name)
                        isConnected = true
                        outgoing.send(Frame.Text(gson.toJson(userInfo)))
                    } else {
                        try {
                            val action = gson.fromJson(text, Action::class.java)
                            when (action.action) {
                                "+1" -> { //TODO валидация
                                    println("buy")
                                    RuntimeStorage.paperCostShift++
                                    userInfo.papers += 1
                                    userInfo.money -= RuntimeStorage.getPaperCost()
                                }
                                "-1" -> { //TODO валидация
                                    println("sell")
                                    RuntimeStorage.paperCostShift--
                                    userInfo.papers -= 1
                                    userInfo.money += RuntimeStorage.getPaperCost()
                                }
                            }
                            for (con in RuntimeStorage.getConnections()) {
                                con.outgoing.send(Frame.Text(gson.toJson(
                                    RuntimeStorage.getUsers()
                                )))
                            }
                        } catch (e: Throwable) {
                            println("Error parse json:\n $text \n")
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                println("onClose ${closeReason.await()}")
            } catch (e: Throwable) {
                println("onError ${closeReason.await()}")
                e.printStackTrace()
            } finally {
                RuntimeStorage.removeConnection(this)
                println("Disconnect.")
            }
        }
    }
}

