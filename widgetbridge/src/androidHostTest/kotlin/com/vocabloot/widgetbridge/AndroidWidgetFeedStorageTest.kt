package com.vocabloot.widgetbridge

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidWidgetFeedStorageTest {
    private val root = Files.createTempDirectory("widgetbridge").toFile()
    private val storage = AndroidWidgetFeedStorage(root)

    @Test
    fun write_promotes_generation_and_points_at_it() {
        storage.writeGeneration("g1", "feed".encodeToByteArray(), listOf(WidgetAsset("a.png", byteArrayOf(9))), keepPrevious = 1)

        val candidates = storage.readGenerationCandidates()
        assertEquals(listOf("g1"), candidates.map { it.id })
        assertEquals("feed", candidates.single().feedBytes.decodeToString())
        assertTrue(File(root, "generations/g1/assets/a.png").isFile)
        assertFalse(File(root, "generations/g1.tmp").exists())
        assertEquals(File(root, "generations/g1/assets/a.png").canonicalPath, storage.assetPath(candidates.single().directoryPath, "a.png"))
    }

    @Test
    fun candidate_order_is_current_then_previous_then_rest_and_prunes_to_keep_previous() {
        listOf("g1", "g2", "g3", "g4").forEach { storage.writeGeneration(it, it.encodeToByteArray(), emptyList(), keepPrevious = 2) }

        assertEquals(listOf("g4", "g3", "g2"), storage.readGenerationCandidates().map { it.id })
        assertFalse(File(root, "generations/g1").exists())
    }

    @Test
    fun temp_and_unsafe_directories_are_ignored() {
        storage.writeGeneration("g1", "x".encodeToByteArray(), emptyList(), keepPrevious = 1)
        File(root, "generations/g9.tmp").mkdirs()
        File(root, "generations/bad name").mkdirs().also { File(root, "generations/bad name/feed.json").writeText("{}") }

        assertEquals(listOf("g1"), storage.readGenerationCandidates().map { it.id })
    }

    @Test
    fun unsafe_or_missing_asset_paths_are_null() {
        storage.writeGeneration("g1", "x".encodeToByteArray(), listOf(WidgetAsset("a.png", byteArrayOf(1))), keepPrevious = 1)
        val dir = storage.readGenerationCandidates().single().directoryPath

        assertNull(storage.assetPath(dir, "../feed.json"))
        assertNull(storage.assetPath(dir, "b.png"))
    }

    @Test
    fun clear_removes_generations_and_pointer() {
        storage.writeGeneration("g1", "x".encodeToByteArray(), emptyList(), keepPrevious = 1)
        storage.clear()
        assertTrue(storage.readGenerationCandidates().isEmpty())
        assertFalse(File(root, "current.json").exists())
    }
}
