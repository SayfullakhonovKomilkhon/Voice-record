package com.example.data.repository

import com.example.data.local.MeetingDao
import com.example.data.model.Meeting
import kotlinx.coroutines.flow.Flow

class MeetingRepository(private val meetingDao: MeetingDao) {
    val allMeetings: Flow<List<Meeting>> = meetingDao.getAllMeetings()

    suspend fun getMeetingById(id: Long): Meeting? = meetingDao.getMeetingById(id)

    suspend fun insertMeeting(meeting: Meeting): Long = meetingDao.insertMeeting(meeting)

    suspend fun updateMeeting(meeting: Meeting) = meetingDao.updateMeeting(meeting)

    suspend fun deleteMeeting(meeting: Meeting) = meetingDao.deleteMeeting(meeting)

    suspend fun deleteMeetingById(id: Long) = meetingDao.deleteMeetingById(id)
}
