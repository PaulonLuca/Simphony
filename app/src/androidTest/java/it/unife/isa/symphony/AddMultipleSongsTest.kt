package it.unife.isa.symphony


import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AddMultipleSongsTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SongsListActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.READ_EXTERNAL_STORAGE"
        )

    @Test
    fun addMultipleSongsTest() {

        val numSongs = (2..5).random()
        for (i in 1..numSongs) {

            val overflowMenuButton = onView(
                allOf(
                    withContentDescription("More options"),
                    childAtPosition(
                        childAtPosition(
                            withId(R.id.toolbar),
                            2
                        ),
                        1
                    ),
                    isDisplayed()
                )
            )
            overflowMenuButton.perform(click())

            val materialTextView = onView(
                allOf(
                    withId(R.id.title), withText("Add song"),
                    childAtPosition(
                        childAtPosition(
                            withId(androidx.appcompat.R.id.content),
                            0
                        ),
                        0
                    ),
                    isDisplayed()
                )
            )
            materialTextView.perform(click())
        }

        onView (withId (R.id.item_list)).check (ViewAssertions.matches (Matchers.withListSize (numSongs)));

    }

    internal object Matchers {
        fun withListSize(size: Int): Matcher<View> {
            return object : TypeSafeMatcher<View>() {
                public override fun matchesSafely(view: View): Boolean {
                    return (view as RecyclerView).size === size
                }

                override fun describeTo(description: Description) {
                    description.appendText("ListView should have $size items")
                }
            }
        }
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
