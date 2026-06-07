/**
 * Calendar Manager for CircleBirthdays 2026
 * Handles rendering of the interactive calendar and integration with Event Data.
 */

let currentDisplayDate = new Date(2026, 0, 1); // Start at Jan 2026

function initCalendar() {
    renderCalendar();
    updateTodayWidget();
}

function changeMonth(delta) {
    currentDisplayDate.setMonth(currentDisplayDate.getMonth() + delta);

    // Keep it within 2026 for this specific feature version
    if (currentDisplayDate.getFullYear() < 2026) {
        currentDisplayDate = new Date(2026, 0, 1);
    } else if (currentDisplayDate.getFullYear() > 2026) {
        currentDisplayDate = new Date(2026, 11, 1);
    }

    renderCalendar();
}

function renderCalendar() {
    const monthDisplay = document.getElementById('current-month-display');
    const daysContainer = document.getElementById('calendar-days');

    if (!monthDisplay || !daysContainer) return;

    const year = currentDisplayDate.getFullYear();
    const month = currentDisplayDate.getMonth();

    const monthNames = ["January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ];

    monthDisplay.innerText = `${monthNames[month]} ${year}`;

    // Clear previous days
    daysContainer.innerHTML = '';

    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    // Previous month padding
    for (let i = 0; i < firstDay; i++) {
        const padding = document.createElement('div');
        padding.className = 'bg-slate-50/50 h-32 md:h-40 border-slate-100';
        daysContainer.appendChild(padding);
    }

    // Current month days
    for (let day = 1; day <= daysInMonth; day++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        const dayElement = createDayElement(day, dateStr);
        daysContainer.appendChild(dayElement);
    }
}

function createDayElement(day, dateStr) {
    const dayDiv = document.createElement('div');
    dayDiv.className = 'bg-white h-32 md:h-40 p-3 border-slate-100 transition-all hover:bg-slate-50 cursor-pointer relative group';

    const d = new Date(dateStr);
    const m = d.getMonth() + 1;
    const date = d.getDate();

    const festival = PanchangData.festivals2026[m]?.[date];

    let content = `<span class="text-sm font-bold ${festival ? 'text-indigo-600' : 'text-slate-400'}">${day}</span>`;

    if (festival) {
        content += `
            <div class="mt-2">
                <span class="block text-[10px] md:text-xs font-bold text-indigo-700 leading-tight bg-indigo-50 p-1 rounded-lg border border-indigo-100 line-clamp-2">
                    ${festival}
                </span>
            </div>
        `;
    }

    dayDiv.innerHTML = content;
    dayDiv.onclick = () => showDayDetails(dateStr);

    return dayDiv;
}

function updateTodayWidget() {
    const today = new Date();
    let displayDate;

    // For demo/2026 purposes, if today isn't 2026, show first day of 2026 or a relevant date
    if (today.getFullYear() !== 2026) {
        displayDate = new Date(2026, 0, 1);
    } else {
        displayDate = today;
    }

    const m = displayDate.getMonth() + 1;
    const date = displayDate.getDate();

    const detailsContainer = document.getElementById('today-panchang-details');
    if (!detailsContainer) return;

    const festival = PanchangData.festivals2026[m]?.[date];

    detailsContainer.innerHTML = `
        <div class="flex items-center gap-4 bg-white/10 p-4 rounded-2xl backdrop-blur-sm cursor-pointer hover:bg-white/20 transition" onclick="showDayDetails('${displayDate.toISOString().split('T')[0]}')">
            <div class="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center text-xl text-white">
                <i class="fas fa-calendar-day"></i>
            </div>
            <div>
                <p class="text-xs text-white/60 font-bold uppercase tracking-widest">Festival</p>
                <p class="font-bold text-white">${festival ? festival : 'No Major Festival'}</p>
            </div>
        </div>
        <div class="flex items-center gap-4 bg-white/10 p-4 rounded-2xl backdrop-blur-sm cursor-pointer hover:bg-white/20 transition">
            <div class="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center text-xl text-white">
                <i class="fas fa-birthday-cake"></i>
            </div>
            <div>
                <p class="text-xs text-white/60 font-bold uppercase tracking-widest">Family Birthdays</p>
                <p class="font-bold text-white">Check Directory</p>
            </div>
        </div>
    `;

    updateUpcomingFestivals(displayDate);
}

function updateUpcomingFestivals(startDate) {
    const container = document.getElementById('upcoming-festivals');
    if (!container) return;

    const upcoming = [];
    const startM = startDate.getMonth() + 1;
    const startD = startDate.getDate();

    // Collect next 5 festivals
    for (let m = startM; m <= 12; m++) {
        const monthFestivals = PanchangData.festivals2026[m];
        if (monthFestivals) {
            const sortedDays = Object.keys(monthFestivals).sort((a, b) => a - b);
            for (const d of sortedDays) {
                if (m === startM && parseInt(d) < startD) continue;
                upcoming.push({ month: m, day: parseInt(d), name: monthFestivals[d] });
                if (upcoming.length >= 5) break;
            }
        }
        if (upcoming.length >= 5) break;
    }

    const monthNamesShort = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

    container.innerHTML = upcoming.map(f => {
        return `
            <div class="flex items-center gap-4 group cursor-pointer hover:bg-slate-50 p-2 rounded-2xl transition" onclick="showDayDetails('2026-${String(f.month).padStart(2, '0')}-${String(f.day).padStart(2, '0')}')">
                <div class="w-12 h-12 bg-indigo-50 rounded-xl flex flex-col items-center justify-center text-indigo-600 border border-indigo-100 group-hover:bg-indigo-600 group-hover:text-white transition">
                    <span class="text-[10px] font-black leading-none uppercase">${monthNamesShort[f.month - 1]}</span>
                    <span class="text-lg font-bold leading-none">${f.day}</span>
                </div>
                <div>
                    <p class="font-bold text-[#2D241E] group-hover:text-indigo-600 transition">${f.name}</p>
                    <p class="text-xs text-slate-400 font-medium">Family Festival</p>
                </div>
            </div>
        `;
    }).join('');
}

function showDayDetails(dateStr) {
    const modal = document.getElementById('calendar-detail-modal');
    if (!modal) return;

    const d = new Date(dateStr);
    const m = d.getMonth() + 1;
    const date = d.getDate();
    const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

    document.getElementById('calendar-modal-date').innerText = `${monthNames[m-1]} ${date}, 2026`;

    const festival = PanchangData.festivals2026[m]?.[date];
    document.getElementById('calendar-modal-title').innerText = festival || "Normal Day";

    const eventDetails = document.getElementById('calendar-event-details');
    if (eventDetails) {
        eventDetails.innerHTML = `
            <div class="p-6 bg-slate-50 rounded-[2rem] border border-slate-100">
                <h4 class="text-xs font-black text-slate-400 uppercase tracking-widest mb-4">Daily Summary</h4>
                <p class="text-slate-600 font-medium">${festival ? `Today we celebrate <strong>${festival}</strong>.` : "No major festivals or family events recorded for this date yet."}</p>
            </div>
        `;
    }

    modal.classList.remove('hidden');
}

function closeCalendarModal() {
    document.getElementById('calendar-detail-modal').classList.add('hidden');
}

window.closeCalendarModal = closeCalendarModal;

// Initialize when the tab is switched to calendar
const originalSwitchTab = window.switchTab;
window.switchTab = function(tabId) {
    if (typeof originalSwitchTab === 'function') {
        originalSwitchTab(tabId);
    }
    if (tabId === 'calendar') {
        initCalendar();
    }
};
