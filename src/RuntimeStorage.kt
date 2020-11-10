package ru.gdcn.game.money

import io.ktor.http.cio.websocket.DefaultWebSocketSession
import ru.gdcn.game.money.library.UserInfo
import java.util.*

object RuntimeStorage {
    private const val START_MONEY = 100
    private const val START_PAPERS_QUANTITY = 0

    private val users = mutableMapOf<String, UserInfo>()
    private val connections = mutableSetOf<DefaultWebSocketSession>()
    private var paperCost = 10
    var paperCostShift = 0
    private val random = Random()

    fun addConnection(connection: DefaultWebSocketSession) = connections.add(connection)

    fun removeConnection(connection: DefaultWebSocketSession) = connections.remove(connection)

    fun getConnections(): Set<DefaultWebSocketSession> = connections

    fun getOrCreateUser(username: String): UserInfo = getUser(username) ?: createUser(username)

    fun getUsers(): Collection<UserInfo> = users.values

    fun getPaperCost(): Int = paperCost

    fun changePaperCost(): Int {
        paperCost = paperCost - 5 + random.nextInt(11) + paperCostShift
        return paperCost
    }

    private fun createUser(username: String): UserInfo {
        val userInfo = UserInfo(username, START_MONEY, START_PAPERS_QUANTITY)
        users[username] = userInfo
        return userInfo
    }

    private fun getUser(username: String): UserInfo? = users[username]
}