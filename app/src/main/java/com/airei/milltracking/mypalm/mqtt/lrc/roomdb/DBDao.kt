package com.airei.milltracking.mypalm.mqtt.lrc.roomdb

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DBDao {
    @Query("SELECT * FROM doors")
    fun getAllDoors(): LiveData<List<DoorTable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg doors: DoorTable) // Accept a variable number of DoorTable objects

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doors: List<DoorTable>)

    @Update
    suspend fun update(door: DoorTable)

    @Update
    suspend fun updateAll(doors: List<DoorTable>)

    @Delete
    suspend fun delete(door: DoorTable)
}