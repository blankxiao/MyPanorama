package cn.szu.blankxiao.panoramaview.api.panorama

import cn.szu.blankxiao.panoramaview.api.common.ListResult
import cn.szu.blankxiao.panoramaview.api.common.Result
import cn.szu.blankxiao.panoramaview.api.panorama.dto.CreateTaskRequestDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageAssetDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageConfirmRequestDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageUploadSignDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageUploadSignRequestDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaTaskDetailDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaTaskListItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PanoramaApi {
    @GET("panorama/health")
    suspend fun health(): Response<Map<String, Any>>

    @POST("panorama/task/create")
    suspend fun createTask(@Body request: CreateTaskRequestDto): Result<Long?>

    @GET("panorama/input-image/my")
    suspend fun listMyInputImages(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): ListResult<PanoramaInputImageAssetDto>

    @POST("panorama/input-image/upload-sign")
    suspend fun uploadInputImageSign(@Body request: PanoramaInputImageUploadSignRequestDto): Result<PanoramaInputImageUploadSignDto?>

    @POST("panorama/input-image/confirm")
    suspend fun confirmInputImageUpload(@Body request: PanoramaInputImageConfirmRequestDto): Result<PanoramaInputImageAssetDto?>

    @GET("panorama/task/list")
    suspend fun listTasks(): Result<List<PanoramaTaskListItemDto>?>

    @GET("panorama/task/detail/{id}")
    suspend fun taskDetail(@Path("id") id: Long): Result<PanoramaTaskDetailDto?>

    @POST("panorama/task/delete")
    suspend fun deleteTask(@Query("taskId") taskId: Long): Result<Unit?>

    @GET("panorama/result/list")
    suspend fun listResults(): Result<List<PanoramaTaskListItemDto>?>

    @GET("panorama/result/{id}")
    suspend fun resultDetail(@Path("id") id: Long): Result<PanoramaTaskDetailDto?>
}
