package com.markedusduplicate.slopboard.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentActionTest {

    @Test
    fun `parses a tap`() {
        assertEquals(AgentAction.Tap(3), AgentAction.parse("""{"action":"tap","index":3}"""))
    }

    @Test
    fun `parses a tap wrapped in prose and code fences`() {
        val raw = "Sure!\n```json\n{\"action\": \"tap\", \"index\": 5}\n```\n"
        assertEquals(AgentAction.Tap(5), AgentAction.parse(raw))
    }

    @Test
    fun `parses a type with text`() {
        assertEquals(
            AgentAction.Type(2, "yes"),
            AgentAction.parse("""{"action":"type","index":2,"text":"yes"}"""),
        )
    }

    @Test
    fun `parses scroll direction, defaulting to down`() {
        assertEquals(AgentAction.Scroll(ScrollDirection.UP), AgentAction.parse("""{"action":"scroll","direction":"up"}"""))
        assertEquals(AgentAction.Scroll(ScrollDirection.DOWN), AgentAction.parse("""{"action":"scroll"}"""))
    }

    @Test
    fun `parses back and done`() {
        assertEquals(AgentAction.Back, AgentAction.parse("""{"action":"back"}"""))
        assertEquals(AgentAction.Done("all set"), AgentAction.parse("""{"action":"done","message":"all set"}"""))
    }

    @Test
    fun `tolerates string index`() {
        assertEquals(AgentAction.Tap(7), AgentAction.parse("""{"action":"tap","index":"7"}"""))
    }

    @Test
    fun `returns Unknown for junk or missing fields`() {
        assertTrue(AgentAction.parse("no json here") is AgentAction.Unknown)
        assertTrue(AgentAction.parse("""{"action":"tap"}""") is AgentAction.Unknown)
        assertTrue(AgentAction.parse("""{"action":"type","index":1}""") is AgentAction.Unknown)
    }
}
