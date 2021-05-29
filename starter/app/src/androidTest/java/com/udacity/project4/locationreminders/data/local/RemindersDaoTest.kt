package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue

import org.junit.Before;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var remindersDao: RemindersDao
    private lateinit var db: RemindersDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<MyApp>()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()
        remindersDao = db.reminderDao()
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

        var reminderFromDb: ReminderDTO? = null

        runBlocking {
            remindersDao.saveReminder(reminder)

            reminderFromDb = remindersDao.getReminderById(reminder.id)
        }
        assertEquals(reminder, reminderFromDb)
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

        var reminderList: List<ReminderDTO>?

        runBlocking {
            //save reminders and test that list of these two is returned by getreminders()
            remindersDao.saveReminder(reminder)
            remindersDao.saveReminder(reminder2)
            reminderList = remindersDao.getReminders()
            assertTrue(reminderList == listOf(reminder, reminder2))

            //test that deleteallreminders makes that getreminders returns an empty list
            remindersDao.deleteAllReminders()
            reminderList = remindersDao.getReminders()

            assertTrue(reminderList!!.isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetDataNotFound() {

        runBlocking {
            val fakeId = "0"
            val reminder = remindersDao.getReminderById(fakeId)
            assertTrue(reminder == null)
        }


    }

}