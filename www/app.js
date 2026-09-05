const { Capacitor } = window;
const MacroPlugin = Capacitor.registerPlugin('MacroPlugin');

let currentSteps = [];
let pendingScheduleSequence = null;

const accStatusEl = document.getElementById('accStatus');
const btnOpenAccessibility = document.getElementById('btnOpenAccessibility');
const btnRecord = document.getElementById('btnRecord');
const btnStop = document.getElementById('btnStop');
const stepsList = document.getElementById('stepsList');
const seqNameInput = document.getElementById('seqName');
const btnSave = document.getElementById('btnSave');
const savedList = document.getElementById('savedList');

const scheduleModal = document.getElementById('scheduleModal');
const btnConfirmSchedule = document.getElementById('btnConfirmSchedule');
const btnCancelSchedule = document.getElementById('btnCancelSchedule');

function loadSequences() {
  const raw = localStorage.getItem('macro_sequences');
  return raw ? JSON.parse(raw) : {};
}

function saveSequences(seqs) {
  localStorage.setItem('macro_sequences', JSON.stringify(seqs));
}

function renderSteps() {
  stepsList.innerHTML = '';
  currentSteps.forEach((step, i) => {
    const li = document.createElement('li');
    const label = step.type === 'tap'
      ? `Toque en (${step.x}, ${step.y})`
      : `Escribir: "${step.text}"`;
    li.innerHTML = `<span>${i + 1}. ${label} <small>+${step.delay}ms</small></span>`;
    stepsList.appendChild(li);
  });
}

function renderSaved() {
  const seqs = loadSequences();
  savedList.innerHTML = '';
  Object.keys(seqs).forEach(name => {
    const li = document.createElement('li');
    li.innerHTML = `
      <span>${name} (${seqs[name].length} pasos)</span>
      <div class="actions">
        <button data-action="play" data-name="${name}">Reproducir</button>
        <button data-action="schedule" data-name="${name}">Programar</button>
        <button data-action="delete" data-name="${name}">Eliminar</button>
      </div>
    `;
    savedList.appendChild(li);
  });
}

async function refreshAccessibilityStatus() {
  try {
    const { enabled } = await MacroPlugin.isAccessibilityEnabled();
    accStatusEl.textContent = enabled ? 'Accesibilidad activada' : 'Accesibilidad NO activada';
    accStatusEl.className = 'status ' + (enabled ? 'ok' : '');
  } catch (e) {
    accStatusEl.textContent = 'No se pudo verificar el permiso';
  }
}

btnOpenAccessibility.addEventListener('click', () => {
  MacroPlugin.openAccessibilitySettings();
});

btnRecord.addEventListener('click', async () => {
  currentSteps = [];
  renderSteps();
  await MacroPlugin.startRecording();
  btnRecord.disabled = true;
  btnStop.disabled = false;
});

btnStop.addEventListener('click', async () => {
  const result = await MacroPlugin.stopRecording();
  currentSteps = result.steps || [];
  renderSteps();
  btnRecord.disabled = false;
  btnStop.disabled = true;
});

btnSave.addEventListener('click', () => {
  const name = seqNameInput.value.trim();
  if (!name || currentSteps.length === 0) return;
  const seqs = loadSequences();
  seqs[name] = currentSteps;
  saveSequences(seqs);
  seqNameInput.value = '';
  renderSaved();
});

savedList.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const { action, name } = btn.dataset;
  const seqs = loadSequences();
  const steps = seqs[name];

  if (action === 'play') {
    await MacroPlugin.playSequence({ steps: JSON.stringify(steps) });
  }

  if (action === 'delete') {
    delete seqs[name];
    saveSequences(seqs);
    renderSaved();
  }

  if (action === 'schedule') {
    pendingScheduleSequence = { name, steps };
    scheduleModal.classList.remove('hidden');
  }
});

btnCancelSchedule.addEventListener('click', () => {
  scheduleModal.classList.add('hidden');
  pendingScheduleSequence = null;
});

btnConfirmSchedule.addEventListener('click', async () => {
  if (!pendingScheduleSequence) return;
  const time = document.getElementById('schedTime').value; // "HH:MM"
  const days = Array.from(document.querySelectorAll('#schedDays input:checked')).map(cb => cb.value);
  if (!time) return;
  const [hour, minute] = time.split(':').map(Number);

  await MacroPlugin.scheduleSequence({
    name: pendingScheduleSequence.name,
    steps: JSON.stringify(pendingScheduleSequence.steps),
    hour,
    minute,
    days: days.join(',')
  });

  scheduleModal.classList.add('hidden');
  pendingScheduleSequence = null;
});

// init
refreshAccessibilityStatus();
renderSaved();
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) refreshAccessibilityStatus();
});
