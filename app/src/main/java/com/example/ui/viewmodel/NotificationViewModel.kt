package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.AppSummaryTuple
import com.example.data.db.CategorySummaryTuple
import com.example.data.model.CapturedNotification
import com.example.data.model.CustomCategory
import com.example.data.repository.NotificationRepository
import com.example.util.NotificationAccessHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterPreset(val label: String) {
    ALL("Todas as Datas"),
    TODAY("Hoje"),
    YESTERDAY("Ontem"),
    LAST_7_DAYS("Últimos 7 dias"),
    LAST_30_DAYS("Últimos 30 dias"),
    CUSTOM("Personalizado")
}

data class DateRangeBounds(
    val start: Long? = null,
    val end: Long? = null
)

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(AppDatabase.getDatabase(application))

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPackage = MutableStateFlow<String?>(null)
    val selectedPackage: StateFlow<String?> = _selectedPackage.asStateFlow()

    private val _datePreset = MutableStateFlow(DateFilterPreset.ALL)
    val datePreset: StateFlow<DateFilterPreset> = _datePreset.asStateFlow()

    private val _customDateBounds = MutableStateFlow(DateRangeBounds())
    val customDateBounds: StateFlow<DateRangeBounds> = _customDateBounds.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    val allCategories: StateFlow<List<CustomCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSummaries: StateFlow<List<AppSummaryTuple>> = repository.appSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryCounts: StateFlow<List<CategorySummaryTuple>> = repository.categoryCounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayCount: StateFlow<Int> = repository.getTodayCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookmarkedNotifications: StateFlow<List<CapturedNotification>> = repository.bookmarkedNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Compute active date boundaries based on preset or custom range
    private val activeDateBounds = combine(_datePreset, _customDateBounds) { preset, custom ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        when (preset) {
            DateFilterPreset.ALL -> DateRangeBounds(null, null)
            DateFilterPreset.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                DateRangeBounds(calendar.timeInMillis, null)
            }
            DateFilterPreset.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                DateRangeBounds(start, calendar.timeInMillis)
            }
            DateFilterPreset.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                DateRangeBounds(calendar.timeInMillis, null)
            }
            DateFilterPreset.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                DateRangeBounds(calendar.timeInMillis, null)
            }
            DateFilterPreset.CUSTOM -> custom
        }
    }

    // Filtered notifications list
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredNotifications: StateFlow<List<CapturedNotification>> = combine(
        _selectedPackage,
        activeDateBounds,
        _selectedCategoryId,
        _searchQuery
    ) { pkg, bounds, catId, query ->
        RepositoryFilterParams(pkg, bounds.start, bounds.end, catId, query)
    }.flatMapLatest { params ->
        repository.getFilteredNotifications(
            packageName = params.packageName,
            fromTimestamp = params.start,
            toTimestamp = params.end,
            categoryId = params.categoryId,
            searchQuery = params.query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAnyFilterActive: StateFlow<Boolean> = combine(
        _selectedPackage,
        _datePreset,
        _selectedCategoryId,
        _searchQuery
    ) { pkg, dateP, catId, query ->
        pkg != null || dateP != DateFilterPreset.ALL || catId != null || query.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkPermission()
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    fun checkPermission() {
        _isPermissionGranted.value = NotificationAccessHelper.isNotificationAccessGranted(getApplication())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectPackage(packageName: String?) {
        _selectedPackage.value = packageName
    }

    fun setDatePreset(preset: DateFilterPreset) {
        _datePreset.value = preset
    }

    fun setCustomDateBounds(start: Long?, end: Long?) {
        _customDateBounds.value = DateRangeBounds(start, end)
        _datePreset.value = DateFilterPreset.CUSTOM
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun clearFilters() {
        _selectedPackage.value = null
        _datePreset.value = DateFilterPreset.ALL
        _customDateBounds.value = DateRangeBounds()
        _selectedCategoryId.value = null
        _searchQuery.value = ""
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleBookmark(id: Long, current: Boolean) {
        viewModelScope.launch {
            repository.setBookmark(id, !current)
        }
    }

    fun deleteNotification(notification: CapturedNotification) {
        viewModelScope.launch {
            repository.deleteNotification(notification)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun deleteByPackage(packageName: String) {
        viewModelScope.launch {
            repository.deleteByPackage(packageName)
            if (_selectedPackage.value == packageName) {
                _selectedPackage.value = null
            }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAll()
            clearFilters()
        }
    }

    fun assignCategory(notificationId: Long, category: CustomCategory?) {
        viewModelScope.launch {
            repository.assignCategory(notificationId, category)
        }
    }

    fun createCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.createCategory(name, colorHex)
        }
    }

    fun updateCategory(category: CustomCategory) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CustomCategory) {
        viewModelScope.launch {
            if (_selectedCategoryId.value == category.id) {
                _selectedCategoryId.value = null
            }
            repository.deleteCategory(category)
        }
    }

    fun sendTestNotification(appName: String, title: String, message: String) {
        NotificationAccessHelper.sendTestNotification(
            context = getApplication(),
            title = title,
            message = message,
            subText = appName
        )
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
        }
    }

    // Helper for formatting section headers
    fun formatSectionHeader(dateKey: String, timestamp: Long): String {
        val todayCal = Calendar.getInstance()
        val notifCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isToday = todayCal.get(Calendar.YEAR) == notifCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.DAY_OF_YEAR) == notifCal.get(Calendar.DAY_OF_YEAR)

        todayCal.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = todayCal.get(Calendar.YEAR) == notifCal.get(Calendar.YEAR) &&
                todayCal.get(Calendar.DAY_OF_YEAR) == notifCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "Hoje"
            isYesterday -> "Ontem"
            else -> {
                val formatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
                formatter.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
            }
        }
    }

    private data class RepositoryFilterParams(
        val packageName: String?,
        val start: Long?,
        val end: Long?,
        val categoryId: Long?,
        val query: String?
    )
}
