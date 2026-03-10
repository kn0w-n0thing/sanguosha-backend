package org.dogcard.websocket

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketConfigTest {

    @LocalServerPort
    private var port: Int = 0

    private fun buildStompClient(): WebSocketStompClient {
        val sockJsClient = SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient())))
        return WebSocketStompClient(sockJsClient)
    }

    @Test
    fun `client can connect to WebSocket endpoint`() {
        val latch = CountDownLatch(1)
        var session: StompSession? = null

        val stompClient = buildStompClient()
        val url = "http://localhost:$port/ws"

        assertDoesNotThrow {
            session = stompClient.connectAsync(url, object : StompSessionHandlerAdapter() {
                override fun afterConnected(s: StompSession, headers: StompHeaders) {
                    latch.countDown()
                }
            }).get(5, TimeUnit.SECONDS)
        }

        assert(latch.await(5, TimeUnit.SECONDS)) { "WebSocket connection timed out" }
        assertNotNull(session)
        session?.disconnect()
    }

    @Test
    fun `client can subscribe to game room topic`() {
        val connectLatch = CountDownLatch(1)
        val subscribeLatch = CountDownLatch(1)

        val stompClient = buildStompClient()
        val url = "http://localhost:$port/ws"
        val roomId = "test-room-1"

        val session = stompClient.connectAsync(url, object : StompSessionHandlerAdapter() {
            override fun afterConnected(s: StompSession, headers: StompHeaders) {
                s.subscribe("/topic/game/$roomId", object : StompFrameHandler {
                    override fun getPayloadType(headers: StompHeaders): Type = String::class.java
                    override fun handleFrame(headers: StompHeaders, payload: Any?) {
                        subscribeLatch.countDown()
                    }
                })
                connectLatch.countDown()
            }
        }).get(5, TimeUnit.SECONDS)

        assert(connectLatch.await(5, TimeUnit.SECONDS)) { "WebSocket connection timed out" }
        session.disconnect()
    }
}