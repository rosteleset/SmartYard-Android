package ru.madbrains.smartyard

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.agoda.kakao.screen.Screen.Companion.idle
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test
import ru.madbrains.data.prefs.SharedPreferenceStorage.Companion.PREFS_NAME
import ru.madbrains.smartyard.screen.AddressScreen
import ru.madbrains.smartyard.screen.AppealScreen
import ru.madbrains.smartyard.screen.BasicSettingsScreen
import ru.madbrains.smartyard.screen.MainScreen
import ru.madbrains.smartyard.screen.NumberRegScreen
import ru.madbrains.smartyard.screen.SettingsScreen
import ru.madbrains.smartyard.screen.SmsRegScreen
import ru.madbrains.smartyard.ui.onboarding.OnboardingActivity

/**
 * @author Nail Shakurov
 * Created on 31.07.2020.
 */

class AuthScreenTest : TestCase() {

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @get:Rule
    val onboardingActivity = ActivityTestRule(OnboardingActivity::class.java)

    fun logoutAccount() {
        run {
            step("Выход из аккаунта") {
                MainScreen.buttomMenu {
                    setSelectedItem(R.id.settings)
                }
                SettingsScreen {
                    ivSettings {
                        click()
                    }
                }
                BasicSettingsScreen {
                    cvExit {
                        click()
                    }
                    alertDialog {
                        positiveButton {
                            click()
                        }
                    }
                }
            }
        }
    }

    fun loginAccount() {
        before {
            val prefs: SharedPreferences =
                InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
            val editor = prefs.edit()
            editor.clear()
            editor.commit()
        }.after {
        }.run {
            step("1. Онбординг ") {
                MainScreen {
                    btnSkipTextView.click()
                }
            }
            step("2. Ввод номера ") {
                NumberRegScreen {
                    tel1 {
                        flakySafely(timeoutMs = 7000) { isVisible() }
                        typeText("912")
                    }
                    tel2 {
                        typeText("345")
                    }
                    tel3 {
                        typeText("6781")
                    }
                }
            }
            step("3. Ввод пин ") {
                SmsRegScreen {
                    pin.click()
                    pin {
                        isVisible()
                        typeText("1001")
                        idle(2000)
                    }
                }

                AppealScreen {
                    nameText {
                        clearText()
                        typeText("Test Name")
                        Espresso.closeSoftKeyboard()
                    }
                    patronymicText {
                        clearText()
                        typeText("Test Partronymic")
                        Espresso.closeSoftKeyboard()
                    }
                    idle(1500)
                    btnDone.click()
                    idle(1500)
                }
            }
        }
    }

    @Test
    fun testBaseSettings() {
        loginAccount()
        run {
            step("Настройки приложения") {
                MainScreen {
                    buttomMenu {
                        setSelectedItem(R.id.settings)
                        idle(1500)
                    }
                }
                SettingsScreen {
                    ivSettings {
                        click()
                        idle(1500)
                    }
                }
                BasicSettingsScreen {
                    tvTitleNotif {
                        click()
                    }
                    swShowNotify {
                        click()
                    }
                    sBalanse {
                        click()
                    }
                    tvShowNotify {
                        click()
                    }
                    swShowNotify {
                        click()
                    }
                    tvShowNotify {
                        click()
                    }
                    tvTitleNotif {
                        click()
                    }
                }
            }
        }
        logoutAccount()
    }

    @Test
    fun check() {
        run {
            loginAccount()
            MainScreen {
                buttomMenu {
                    setSelectedItem(R.id.address)
                    idle(1500)
                    setSelectedItem(R.id.pay)
                    idle(1500)
                    setSelectedItem(R.id.address)
                    idle(1500)
                }
            }
            AddressScreen {
                rv_parent {
                    isVisible()
                    idle(1500)
                    childAt<AddressScreen.Item>(1) {
                        isVisible()
                        click()
                        tbOpen {
                            idle(1500)
                        }
                    }
                }
            }
            logoutAccount()
        }
    }
}
