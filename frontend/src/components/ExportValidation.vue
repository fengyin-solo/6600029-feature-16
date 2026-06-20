<script setup lang="ts">
import { computed } from 'vue';
import { useDroneStore } from '../store/drone';
import type { RiskItem, RiskCategory, RiskLevel } from '../types';

const store = useDroneStore();

const emit = defineEmits<{
  (e: 'confirm'): void;
  (e: 'cancel'): void;
}>(); 

const summary = computed(() => store.exportValidationResult);

const overallBadge = computed(() => {
  const level = summary.value?.overallLevel || 'safe';
  const map: Record<RiskLevel, { text: string; bg: string; icon: string }> = {
    safe: { text: '校验通过', bg: 'bg-emerald-600', icon: '✓' },
    warning: { text: '存在警告', bg: 'bg-amber-600', icon: '⚠' },
    danger: { text: '存在严重风险', bg: 'bg-red-600', icon: '✗' },
  };
  return map[level];
});

function categoryIcon(cat: RiskCategory): string {
  const map: Record<RiskCategory, string> = {
    battery: '🔋',
    noFlyZone: '🚫',
    altitude: '⛰',
    terrain: '🏔',
  };
  return map[cat];
}

function categoryLabel(cat: RiskCategory): string {
  const map: Record<RiskCategory, string> = {
    battery: '电量',
    noFlyZone: '禁飞区',
    altitude: '高度限制',
    terrain: '地形',
  };
  return map[cat];
}

function riskBg(level: RiskLevel): string {
  const map: Record<RiskLevel, string> = {
    safe: 'border-slate-700 bg-slate-900',
    warning: 'border-amber-500/40 bg-amber-950/30',
    danger: 'border-red-500/40 bg-red-950/30',
  };
  return map[level];
}

function riskBadge(level: RiskLevel): { text: string; cls: string } {
  const map: Record<RiskLevel, { text: string; cls: string }> = {
    safe: { text: '安全', cls: 'bg-emerald-600' },
    warning: { text: '警告', cls: 'bg-amber-600' },
    danger: { text: '危险', cls: 'bg-red-600' },
  };
  return map[level];
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}分${s}秒`;
}

function handleConfirm() {
  emit('confirm');
}

function handleCancel() {
  emit('cancel');
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="store.showExportDialog && summary"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm"
      @click.self="handleCancel"
    >
      <div class="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-[520px] max-w-[92vw] max-h-[88vh] flex flex-col overflow-hidden">
        <!-- Header -->
        <div class="px-5 py-4 border-b border-slate-800 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="text-2xl">📋</div>
            <div>
              <h2 class="text-base font-bold text-slate-100">航线导出校验摘要</h2>
              <p class="text-xs text-slate-400 mt-0.5">请在下载前审阅以下风险</p>
            </div>
          </div>
          <span :class="[overallBadge.bg, 'px-3 py-1 rounded-full text-xs font-semibold text-white flex items-center gap-1']">
            <span>{{ overallBadge.icon }}</span>
            <span>{{ overallBadge.text }}</span>
          </span>
        </div>

        <!-- Body -->
        <div class="flex-1 overflow-y-auto p-5 space-y-4">
          <!-- Overview Stats -->
          <div class="grid grid-cols-4 gap-2 text-center">
            <div class="bg-slate-800/60 rounded-lg p-2.5">
              <div class="text-[10px] text-slate-400">航点数</div>
              <div class="text-lg font-bold text-sky-400">{{ store.waypoints.length }}</div>
            </div>
            <div class="bg-slate-800/60 rounded-lg p-2.5">
              <div class="text-[10px] text-slate-400">距离</div>
              <div class="text-lg font-bold text-sky-400">{{ (store.totalDistance / 1000).toFixed(2) }}<span class="text-[10px] text-slate-500 ml-0.5">km</span></div>
            </div>
            <div class="bg-slate-800/60 rounded-lg p-2.5">
              <div class="text-[10px] text-slate-400">时长</div>
              <div class="text-lg font-bold text-sky-400">{{ formatTime(store.estimatedTime) }}</div>
            </div>
            <div class="bg-slate-800/60 rounded-lg p-2.5">
              <div class="text-[10px] text-slate-400">耗电</div>
              <div class="text-lg font-bold" :class="store.batteryPercent >= 75 ? 'text-red-400' : store.batteryPercent >= 50 ? 'text-amber-400' : 'text-emerald-400'">
                {{ store.batteryPercent.toFixed(0) }}<span class="text-[10px] text-slate-500 ml-0.5">%</span>
              </div>
            </div>
          </div>

          <!-- Risk Summary Bar -->
          <div class="flex items-center justify-between bg-slate-800/60 rounded-lg px-3 py-2 text-xs">
            <div class="flex items-center gap-4">
              <span class="flex items-center gap-1">
                <span class="w-2.5 h-2.5 rounded-full bg-red-500"></span>
                <span class="text-slate-300">严重:</span>
                <span class="font-bold text-red-400">{{ summary.dangerCount }}</span>
              </span>
              <span class="flex items-center gap-1">
                <span class="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
                <span class="text-slate-300">警告:</span>
                <span class="font-bold text-amber-400">{{ summary.warningCount }}</span>
              </span>
              <span class="flex items-center gap-1">
                <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                <span class="text-slate-300">安全:</span>
                <span class="font-bold text-emerald-400">{{ 4 - summary.totalRiskCount }}</span>
              </span>
            </div>
            <div class="text-slate-500">
              共 {{ summary.totalRiskCount }} 项提示
            </div>
          </div>

          <!-- Risk List -->
          <div class="space-y-2.5">
            <template v-for="risk in summary.risks" :key="risk.category">
              <div :class="[riskBg(risk.level), 'border rounded-lg p-3']">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex items-start gap-2.5 flex-1">
                    <span class="text-xl mt-0.5">{{ categoryIcon(risk.category) }}</span>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 flex-wrap">
                        <span class="font-semibold text-sm text-slate-100">{{ risk.title }}</span>
                        <span :class="[riskBadge(risk.level).cls, 'px-1.5 py-0.5 rounded text-[10px] font-semibold text-white']">
                          {{ riskBadge(risk.level).text }}
                        </span>
                        <span class="text-[10px] text-slate-500 bg-slate-800 px-1.5 py-0.5 rounded">
                          {{ categoryLabel(risk.category) }}
                        </span>
                      </div>
                      <p class="text-xs text-slate-400 mt-1">{{ risk.description }}</p>
                      <p v-if="risk.detail" class="text-[11px] text-slate-500 mt-1.5 italic">
                        {{ risk.detail }}
                      </p>
                      <div
                        v-if="risk.affectedWaypoints && risk.affectedWaypoints.length > 0"
                        class="mt-2 flex flex-wrap gap-1"
                      >
                        <span
                          v-for="(wid, i) in risk.affectedWaypoints.slice(0, 8)"
                          :key="wid"
                          class="text-[9px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded"
                        >
                          {{ wid }}
                        </span>
                        <span
                          v-if="risk.affectedWaypoints.length > 8"
                          class="text-[9px] bg-slate-800 text-slate-500 px-1.5 py-0.5 rounded"
                        >
                          +{{ risk.affectedWaypoints.length - 8 }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <!-- Safe placeholder -->
            <div
              v-if="summary.risks.length === 0"
              class="border border-emerald-700/50 bg-emerald-950/30 rounded-lg p-5 text-center"
            >
              <div class="text-4xl mb-2">✅</div>
              <div class="text-sm font-semibold text-emerald-400">航线完全符合安全规范</div>
              <div class="text-xs text-slate-400 mt-1">电量、禁飞区、高度、地形四项校验均通过</div>
            </div>
          </div>

          <!-- Danger Notice -->
          <div
            v-if="summary.dangerCount > 0"
            class="bg-red-950/40 border border-red-800/50 rounded-lg p-3 text-xs text-red-300"
          >
            <div class="font-semibold text-red-400 mb-1 flex items-center gap-1.5">
              <span>⚠️</span>
              <span>重要提示</span>
            </div>
            <p>
              当前航线存在 {{ summary.dangerCount }} 项严重风险，继续导出可能导致飞行事故或违反法规。
              建议调整航线后重新校验。
            </p>
          </div>
        </div>

        <!-- Footer -->
        <div class="border-t border-slate-800 px-5 py-3.5 flex items-center gap-2 bg-slate-900/80">
          <button
            @click="handleCancel"
            class="flex-1 py-2 rounded-lg text-xs font-medium bg-slate-700 text-slate-200 hover:bg-slate-600 transition"
          >
            取消
          </button>
          <button
            @click="handleConfirm"
            :class="[
              'flex-1 py-2 rounded-lg text-xs font-medium transition',
              summary.dangerCount > 0
                ? 'bg-red-700 text-white hover:bg-red-600'
                : 'bg-emerald-700 text-white hover:bg-emerald-600'
            ]"
          >
            {{ summary.dangerCount > 0 ? '仍要导出（风险自负）' : '确认导出' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
