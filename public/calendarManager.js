/**
 * Calendar Manager for CircleBirthdays 2026
 * Handles rendering of the interactive calendar and integration with PanchangData.
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

function switchCalendarView(view) {
    const gridView = document.getElementById('calendar-grid-view');
    const paperView = document.getElementById('calendar-paper-view');
    const gridBtn = document.getElementById('view-btn-grid');
    const paperBtn = document.getElementById('view-btn-paper');

    if (view === 'grid') {
        gridView.classList.remove('hidden');
        paperView.classList.add('hidden');
        gridBtn.classList.add('bg-white', 'shadow-sm', 'text-indigo-600');
        gridBtn.classList.remove('text-slate-500');
        paperBtn.classList.remove('bg-white', 'shadow-sm', 'text-indigo-600');
        paperBtn.classList.add('text-slate-500');
    } else {
        gridView.classList.add('hidden');
        paperView.classList.remove('hidden');
        paperBtn.classList.add('bg-white', 'shadow-sm', 'text-indigo-600');
        paperBtn.classList.remove('text-slate-500');
        gridBtn.classList.remove('bg-white', 'shadow-sm', 'text-indigo-600');
        gridBtn.classList.add('text-slate-500');
    }
}

window.switchCalendarView = switchCalendarView;

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

    // Update Reference Image
    const imgElement = document.getElementById('monthly-panchang-img');
    const smallImgElement = document.getElementById('monthly-panchang-img-small');
    if (imgElement) {
        imgElement.src = `calendar/${month + 1}.jpg`;
    }
    if (smallImgElement) {
        smallImgElement.src = `calendar/${month + 1}.jpg`;
    }

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
    const panchakEntry = PanchangData.panchak2026[m];
    const isPanchak = panchakEntry ? panchakEntry.some(range => date >= range[0] && date <= range[1]) : false;

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

    if (isPanchak) {
        content += `
            <div class="absolute bottom-2 left-2 right-2">
                <span class="block text-[8px] font-black text-amber-600 uppercase tracking-tighter">Panchak</span>
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
    const muhurats = PanchangData.muhurats2026[m];

    let muhuratText = 'Check Details';
    if (muhurats) {
        for (const [type, days] of Object.entries(muhurats)) {
            if (days.includes(date)) {
                muhuratText = type;
                break;
            }
        }
    }

    detailsContainer.innerHTML = `
        <div class="flex items-center gap-4 bg-white/10 p-4 rounded-2xl backdrop-blur-sm cursor-pointer hover:bg-white/20 transition" onclick="showDayDetails('${displayDate.toISOString().split('T')[0]}')">
            <div class="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center text-xl text-white">
                <i class="fas fa-om"></i>
            </div>
            <div>
                <p class="text-xs text-white/60 font-bold uppercase tracking-widest">Festival</p>
                <p class="font-bold text-white">${festival ? festival : 'No Major Festival'}</p>
            </div>
        </div>
        <div class="flex items-center gap-4 bg-white/10 p-4 rounded-2xl backdrop-blur-sm cursor-pointer hover:bg-white/20 transition" onclick="showDayDetails('${displayDate.toISOString().split('T')[0]}')">
            <div class="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center text-xl text-white">
                <i class="fas fa-clock"></i>
            </div>
            <div>
                <p class="text-xs text-white/60 font-bold uppercase tracking-widest">Muhurat</p>
                <p class="font-bold text-white">${muhuratText}</p>
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
                    <p class="text-xs text-slate-400 font-medium">Auspicious Day</p>
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

    // Panchak
    const panchakEntry = PanchangData.panchak2026[m];
    const isPanchak = panchakEntry ? panchakEntry.some(range => date >= range[0] && date <= range[1]) : false;
    const panchakWarning = document.getElementById('calendar-panchak-warning');
    if (isPanchak) {
        panchakWarning.classList.remove('hidden');
    } else {
        panchakWarning.classList.add('hidden');
    }

    // Muhurats
    const muhuratSection = document.getElementById('calendar-muhurat-section');
    const muhuratList = document.getElementById('calendar-muhurat-list');
    const monthMuhurats = PanchangData.muhurats2026[m];
    let availableMuhurats = [];

    if (monthMuhurats) {
        for (const [type, days] of Object.entries(monthMuhurats)) {
            if (days.includes(date)) {
                availableMuhurats.push(type);
            }
        }
    }

    if (availableMuhurats.length > 0) {
        muhuratSection.classList.remove('hidden');
        muhuratList.innerHTML = availableMuhurats.map(m => `
            <div class="bg-indigo-50 p-4 rounded-2xl flex items-center justify-between border border-indigo-100">
                <span class="font-bold text-indigo-900">${m}</span>
                <span class="text-[10px] font-black text-indigo-400 uppercase tracking-widest">Auspicious Time</span>
            </div>
        `).join('');
    } else {
        muhuratSection.classList.add('hidden');
    }

    // Mock Panchang Details (Tithi, Nakshatra etc based on date)
    const panchangSection = document.getElementById('calendar-panchang-section');
    const panchangDetails = document.getElementById('calendar-panchang-details');
    panchangSection.classList.remove('hidden');

    // Simple pseudo-random logic for mock data consistent with the date
    const tithis = ["Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami", "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami", "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima", "Amavasya"];
    const tithi = tithis[date % tithis.length];

    panchangDetails.innerHTML = `
        <div class="bg-slate-50 p-3 rounded-xl">
            <p class="text-[8px] font-black text-slate-400 uppercase tracking-widest mb-1">Tithi</p>
            <p class="text-sm font-bold text-slate-700">${tithi}</p>
        </div>
        <div class="bg-slate-50 p-3 rounded-xl">
            <p class="text-[8px] font-black text-slate-400 uppercase tracking-widest mb-1">Nakshatra</p>
            <p class="text-sm font-bold text-slate-700">Ashwini</p>
        </div>
        <div class="bg-slate-50 p-3 rounded-xl">
            <p class="text-[8px] font-black text-slate-400 uppercase tracking-widest mb-1">Yoga</p>
            <p class="text-sm font-bold text-slate-700">Siddha</p>
        </div>
        <div class="bg-slate-50 p-3 rounded-xl">
            <p class="text-[8px] font-black text-slate-400 uppercase tracking-widest mb-1">Karana</p>
            <p class="text-sm font-bold text-slate-700">Bava</p>
        </div>
    `;

    modal.classList.remove('hidden');
}

function closeCalendarModal() {
    document.getElementById('calendar-detail-modal').classList.add('hidden');
}

function openPanchangImageModal() {
    const modal = document.getElementById('panchang-image-modal');
    const modalImg = document.getElementById('modal-panchang-img');
    const sourceImg = document.getElementById('monthly-panchang-img');

    if (modal && modalImg && sourceImg) {
        modalImg.src = sourceImg.src;
        modal.classList.remove('hidden');
        document.body.style.overflow = 'hidden'; // Prevent scrolling
    }
}

function closePanchangImageModal() {
    const modal = document.getElementById('panchang-image-modal');
    if (modal) {
        modal.classList.add('hidden');
        document.body.style.overflow = ''; // Restore scrolling
    }
}

window.closeCalendarModal = closeCalendarModal;
window.openPanchangImageModal = openPanchangImageModal;
window.closePanchangImageModal = closePanchangImageModal;

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
