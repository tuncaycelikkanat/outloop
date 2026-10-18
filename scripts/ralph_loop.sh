#!/usr/bin/env bash
# ==============================================================================
# Ralph Loop (Ralph Wiggum Pattern) for No More Reels
# Powered by GSD Core (Get Shit Done)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
GSD_DIR="$PROJECT_ROOT/.gsd"
TASKS_FILE="$GSD_DIR/tasks.json"
PROGRESS_LOG="$GSD_DIR/progress.log"

cd "$PROJECT_ROOT"

echo "=========================================================="
echo "🔄 Ralph Loop başlatılıyor... (GSD Core Engine)"
echo "Hedef Proje: No More Reels"
echo "Görev Kuyruğu: $TASKS_FILE"
echo "=========================================================="

if [[ ! -f "$TASKS_FILE" ]]; then
  echo "Hata: $TASKS_FILE bulunamadı!"
  exit 1
fi

mkdir -p "$GSD_DIR"
touch "$PROGRESS_LOG"

# Yardımcı Python fonksiyonu ile sıradaki pending görevi al
get_next_task() {
  python3 -c "
import json
with open('$TASKS_FILE', 'r') as f:
    tasks = json.load(f)
for task in tasks:
    if task.get('status') == 'pending':
        print(f\"{task['id']}|{task['title']}\")
        break
"
}

# Görev durumunu güncelle
mark_task_completed() {
  local task_id="$1"
  python3 -c "
import json
with open('$TASKS_FILE', 'r') as f:
    tasks = json.load(f)
for task in tasks:
    if task.get('id') == '$task_id':
        task['status'] = 'completed'
with open('$TASKS_FILE', 'w') as f:
    json.dump(tasks, f, indent=2, ensure_ascii=False)
"
}

iteration=1
while true; do
  echo ""
  echo "----------------------------------------------------------"
  echo "📍 Ralph Döngüsü #$iteration"
  echo "----------------------------------------------------------"

  TASK_INFO="$(get_next_task)"

  if [[ -z "$TASK_INFO" ]]; then
    echo "🎉 Kuyrukta bekleyen tüm görevler tamamlandı!"
    echo "Ralph Loop başarıyla sona erdi."
    exit 0
  fi

  TASK_ID="${TASK_INFO%%|*}"
  TASK_TITLE="${TASK_INFO##*|}"

  echo "🎯 Sıradaki Görev: [$TASK_ID] $TASK_TITLE"
  echo "⏰ Başlangıç Zamanı: $(date '+%Y-%m-%d %H:%M:%S')"

  # Görev bağlamını göster
  echo "📋 Şartname kontrol ediliyor (.gsd/SPEC.md)..."
  
  # Not: Burada ajan aracı (Antigravity CLI / Cursor / Claude Code) veya
  # ilgili görev çalıştırılır. Döngü, ajanın işini bitirip testleri geçmesini bekler.
  echo "🛠️ Doğrulama testleri çalıştırılıyor..."
  if ./gradlew testDebugUnitTest --quiet; then
    echo "✅ Testler başarıyla geçti!"
    
    # Görevi tamamlandı olarak işaretle
    mark_task_completed "$TASK_ID"
    
    # İlerleme günlüğüne ekle
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] COMPLETED: $TASK_ID - $TASK_TITLE" >> "$PROGRESS_LOG"
    
    # Git commit at (değişiklik varsa)
    if [[ -n "$(git status --porcelain app/ .gsd/)" ]]; then
      git add app/ .gsd/
      git commit -m "feat(gsd): $TASK_ID - $TASK_TITLE" || true
      echo "📦 Değişiklikler Git'e kaydedildi (Commit atıldı)."
    fi

    echo "✨ Görev [$TASK_ID] tamamlandı."
  else
    echo "❌ HATA: Testler başarısız oldu! Görev tamamlanamadı."
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] FAILED: $TASK_ID - $TASK_TITLE" >> "$PROGRESS_LOG"
    echo "Lütfen kodu kontrol edin veya ajandan düzeltme isteyin."
    exit 1
  fi

  iteration=$((iteration + 1))
  sleep 1
done
