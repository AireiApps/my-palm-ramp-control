package com.airei.milltracking.mypalm.mqtt.lrc.roomdb

import javax.inject.Inject

class DBRepository @Inject constructor(
    private val databaseDao: DBDao,
) {


    fun getAllDoors() = databaseDao.getAllDoors()

    // Function to insert doors into the database
    suspend fun insertAll(doors: List<DoorTable>) {
        databaseDao.insertAll(doors)
    }

    suspend fun updateList(doors: List<DoorTable>) {
        databaseDao.updateAll(doors)
    }

    suspend fun updateDoor(door: DoorTable) {
        databaseDao.update(door)
    }

    // Function to delete a door
    suspend fun deleteDoor(door: DoorTable) {
        databaseDao.delete(door)
    }
}