package com.praharsh.infotainmentlogconsole

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationOpensSettingsAndSavedLogs() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("API & session").assertIsDisplayed()

        composeRule.onNodeWithText("Saved").performClick()
        composeRule.onNodeWithText("Saved logs").assertIsDisplayed()
    }
}
