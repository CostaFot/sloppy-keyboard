package com.feelsokman.androidtemplate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.feelsokman.androidtemplate.ui.activity.MainActivity
import dagger.hilt.EntryPoints
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainActivityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /**
     * Use the primary activity to initialize the app normally.
     */
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // This is how to reach into SingletonComponent for the test application
        // prefer TestInstallIn imo
        val appInitializer = EntryPoints.get(
            composeTestRule.activity.application,
            ApplicationEntryPoint::class.java
        ).appInitializer()

    }

    @Test
    fun firstScreen_isForYou() {
        composeTestRule.apply {
            onNodeWithText("Hello").assertIsDisplayed()
        }
    }
}
