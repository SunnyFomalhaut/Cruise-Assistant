package com.cruise.assistant.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.cruise.assistant.data.db.entity.DayPlanEntity
import com.cruise.assistant.data.db.entity.VoyageEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface VoyageDao {
    @Query("SELECT * FROM voyage WHERE id = 1")
    fun observeVoyage(): Flow<VoyageEntity?>

    @Query("SELECT * FROM voyage WHERE id = 1")
    suspend fun getVoyage(): VoyageEntity?

    @Upsert
    suspend fun upsertVoyage(voyage: VoyageEntity)

    @Query("DELETE FROM voyage")
    suspend fun clearVoyage()

    @Query("SELECT * FROM day_plan ORDER BY date")
    fun observeDayPlans(): Flow<List<DayPlanEntity>>

    @Query("SELECT * FROM day_plan ORDER BY date")
    suspend fun observeDayPlansOnce(): List<DayPlanEntity>

    @Query("SELECT * FROM day_plan WHERE date = :date")
    fun observeDayPlan(date: LocalDate): Flow<DayPlanEntity?>

    @Upsert
    suspend fun upsertDayPlans(plans: List<DayPlanEntity>)

    @Upsert
    suspend fun upsertDayPlan(plan: DayPlanEntity)

    @Query("DELETE FROM day_plan")
    suspend fun clearDayPlans()

    @Delete
    suspend fun deleteDayPlan(plan: DayPlanEntity)
}
