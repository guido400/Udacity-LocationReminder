package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var localRepository: RemindersLocalRepository
    private lateinit var db: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<MyApp>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()
        localRepository = RemindersLocalRepository ( db.reminderDao(), Dispatchers.Main)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testInsertAndGetReminder() {
        val reminder = ReminderDTO(
            "test_title1",
            "test_description",
            "testlocation",
            0.1,
            0.1
        )


        runBlocking {
            localRepository.saveReminder(reminder)

            val result = localRepository.getReminder(reminder.id)
            assert(result is Result.Success<ReminderDTO>)

            val successResult = result as Result.Success<ReminderDTO>
            assertEquals (successResult.data.title,"test_title1")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetRemindersAndDeleteAll() {
        val reminder = ReminderDTO(
            "test_title1",
            "test_description",
            "testlocation",
            0.1,
            0.1
        )

        val reminder2 = ReminderDTO(
            "test_title2",
            "test_description",
            "testlocation",
            0.1,
            0.1
        )

        runBlocking {
            //save reminders and test that list of these two is returned by getreminders()
            localRepository.saveReminder(reminder)
            localRepository.saveReminder(reminder2)
            var result = localRepository.getReminders()

            assert(result is Result.Success<List<ReminderDTO>>)

            var successResult = result as Result.Success<List<ReminderDTO>>
            assertTrue(successResult.data == listOf(reminder, reminder2))

            //test that deleteallreminders makes that getreminders returns an empty list
            localRepository.deleteAllReminders()
            result = localRepository.getReminders()

            successResult = result as Result.Success<List<ReminderDTO>>

            assertTrue(successResult.data.isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testReminderNotExist() {
        runBlocking {
            val result = localRepository.getReminder("fakeId")
            assert(result is Result.Error)
        }
    }

}