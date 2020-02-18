/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-3
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.utils.models.Date
import java.text.DecimalFormat

class LibrusApiBehaviourGrades(override val data: DataLibrus,
                               override val lastSync: Long?,
                               val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiBehaviourGrades"
    }

    private val nameFormat by lazy { DecimalFormat("#.##") }

    init { data.profile?.also { profile ->
        apiGet(TAG, "BehaviourGrades/Points") { json ->

            if (data.startPointsSemester1 > 0) {
                val semester1StartGradeObject = Grade(
                        profileId,
                        -101,
                        data.app.getString(R.string.grade_start_points),
                        0xffbdbdbd.toInt(),
                        data.app.getString(R.string.grade_start_points_format, 1),
                        nameFormat.format(data.startPointsSemester1),
                        data.startPointsSemester1.toFloat(),
                        -1f,
                        1,
                        -1,
                        1
                ).apply { type = Grade.TYPE_POINT_SUM }

                data.gradeList.add(semester1StartGradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_GRADE,
                        semester1StartGradeObject.id,
                        true,
                        true,
                        profile.getSemesterStart(1).inMillis
                ))
            }

            if (data.startPointsSemester2 > 0) {
                val semester2StartGradeObject = Grade(
                        profileId,
                        -102,
                        data.app.getString(R.string.grade_start_points),
                        0xffbdbdbd.toInt(),
                        data.app.getString(R.string.grade_start_points_format, 2),
                        nameFormat.format(data.startPointsSemester2),
                        data.startPointsSemester2.toFloat(),
                        -1f,
                        2,
                        -1,
                        1
                ).apply { type = Grade.TYPE_POINT_SUM }

                data.gradeList.add(semester2StartGradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_GRADE,
                        semester2StartGradeObject.id,
                        true,
                        true,
                        profile.getSemesterStart(2).inMillis
                ))
            }

            json.getJsonArray("Grades")?.asJsonObjectList()?.forEach { grade ->
                val id = grade.getLong("Id") ?: return@forEach
                val value = grade.getFloat("Value")
                val shortName = grade.getString("ShortName")
                val semester = grade.getInt("Semester") ?: profile.currentSemester
                val teacherId = grade.getJsonObject("AddedBy")?.getLong("Id") ?: -1
                val addedDate = grade.getString("AddDate")?.let { Date.fromIso(it) }
                        ?: System.currentTimeMillis()

                val name = when {
                    value != null -> (if (value >= 0) "+" else "") + nameFormat.format(value)
                    shortName != null -> shortName
                    else -> return@forEach
                }

                val color = data.getColor(when {
                    value == null || value == 0f -> 12
                    value > 0 -> 16
                    value < 0 -> 26
                    else -> 12
                })

                val categoryId = grade.getJsonObject("Category")?.getLong("Id") ?: -1
                val category = data.gradeCategories.singleOrNull {
                    it.categoryId == categoryId && it.type == GradeCategory.TYPE_BEHAVIOUR
                }

                val categoryName = category?.text ?: ""

                val description = grade.getJsonArray("Comments")?.asJsonObjectList()?.let { comments ->
                    if (comments.isNotEmpty()) {
                        data.gradeCategories.singleOrNull {
                            it.type == GradeCategory.TYPE_BEHAVIOUR_COMMENT
                                    && it.categoryId == comments[0].asJsonObject.getLong("Id")
                        }?.text
                    } else null
                } ?: ""

                val valueFrom = value ?: category?.valueFrom ?: 0f
                val valueTo = category?.valueTo ?: 0f

                val gradeObject = Grade(
                        profileId,
                        id,
                        categoryName,
                        color,
                        description,
                        name,
                        valueFrom,
                        -1f,
                        semester,
                        teacherId,
                        1
                ).apply {
                    type = Grade.TYPE_POINT_SUM
                    valueMax = valueTo
                }

                data.gradeList.add(gradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_GRADE,
                        id,
                        profile.empty,
                        profile.empty,
                        addedDate
                ))
            }

            data.toRemove.add(DataRemoveModel.Grades.semesterWithType(profile.currentSemester, Grade.TYPE_POINT_SUM))
            data.setSyncNext(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES)
        }
    } ?: onSuccess(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES) }
}
