# -*- coding: utf-8 -*-
"""Génère src/locale/messages.<lang>.xlf à partir de la table TRANSLATIONS.

Usage : python scripts/gen-i18n-xlf.py   (depuis apps/web)

Pourquoi un générateur plutôt que des XLF édités à la main : une seule source
de vérité par ID, diff lisibles, et l'ajout d'une langue = une colonne.
Les IDs sont explicites (@@nav.*, @@shell.*) — pas de dépendance à l'ordre
d'extraction. `ng extract-i18n` reste utilisable pour détecter les oublis.
"""
import io
import os

LANGS = ["en", "es", "ar", "ja", "zh"]

# id: (source FR, en, es, ar, ja, zh)
TRANSLATIONS = {
    # ---- shell -----------------------------------------------------------------
    "shell.search.placeholder": (
        "Rechercher : modules, normes, KPI, documents…",
        "Search: modules, standards, KPIs, documents…",
        "Buscar: módulos, normas, KPI, documentos…",
        "بحث: الوحدات، المعايير، المؤشرات، المستندات…",
        "検索：モジュール、規格、KPI、文書…",
        "搜索：模块、标准、KPI、文档…",
    ),
    "shell.search.aria": (
        "Recherche globale", "Global search", "Búsqueda global",
        "بحث شامل", "全体検索", "全局搜索",
    ),
    # ---- navigation -------------------------------------------------------------
    "nav.pilotage": ("Pilotage", "Steering", "Pilotaje", "القيادة", "ダッシュボード", "驾驶舱"),
    "nav.tableau-de-bord": ("Tableau de bord", "Dashboard", "Panel de control", "لوحة المعلومات", "ダッシュボード", "仪表盘"),
    "nav.accueil": ("Accueil", "Home", "Inicio", "الرئيسية", "ホーム", "首页"),
    "nav.mes-dashboards": ("Mes dashboards", "My dashboards", "Mis paneles", "لوحاتي", "マイダッシュボード", "我的仪表盘"),
    "nav.indicateurs-kpi": ("Indicateurs (KPI)", "Indicators (KPIs)", "Indicadores (KPI)", "المؤشرات (KPI)", "指標（KPI）", "指标（KPI）"),
    "nav.assistant-ia": ("Assistant IA", "AI Assistant", "Asistente IA", "مساعد الذكاء الاصطناعي", "AIアシスタント", "AI 助手"),
    "nav.methodes-qualite": ("Méthodes qualité", "Quality methods", "Métodos de calidad", "أساليب الجودة", "品質手法", "质量方法"),
    "nav.pdca": ("PDCA", "PDCA", "PDCA", "PDCA", "PDCA", "PDCA"),
    "nav.ishikawa": ("Ishikawa", "Ishikawa", "Ishikawa", "إيشيكاوا", "特性要因図", "鱼骨图"),
    "nav.5s": ("5S", "5S", "5S", "5S", "5S", "5S"),
    "nav.dmaic": ("DMAIC", "DMAIC", "DMAIC", "DMAIC", "DMAIC", "DMAIC"),
    "nav.spc": ("SPC", "SPC", "SPC", "SPC", "SPC", "SPC"),
    "nav.cercles": ("Cercles", "Quality circles", "Círculos", "حلقات الجودة", "QCサークル", "质量圈"),
    "nav.qualite-operationnelle": ("Qualité opérationnelle", "Operational quality", "Calidad operativa", "الجودة التشغيلية", "オペレーション品質", "运营质量"),
    "nav.capa": ("CAPA", "CAPA", "CAPA", "CAPA", "CAPA", "CAPA"),
    "nav.audits": ("Audits", "Audits", "Auditorías", "التدقيقات", "監査", "审核"),
    "nav.risques-fmea": ("Risques (FMEA)", "Risks (FMEA)", "Riesgos (AMFE)", "المخاطر (FMEA)", "リスク（FMEA）", "风险（FMEA）"),
    "nav.documents": ("Documents", "Documents", "Documentos", "المستندات", "文書", "文档"),
    "nav.changements": ("Changements", "Changes", "Cambios", "التغييرات", "変更管理", "变更"),
    "nav.ehs": ("EHS", "EHS", "EHS", "البيئة والصحة والسلامة", "EHS", "EHS"),
    "nav.fournisseurs-competences": ("Fournisseurs & compétences", "Suppliers & skills", "Proveedores y competencias", "الموردون والكفاءات", "サプライヤーとスキル", "供应商与能力"),
    "nav.fournisseurs": ("Fournisseurs", "Suppliers", "Proveedores", "الموردون", "サプライヤー", "供应商"),
    "nav.formation": ("Formation", "Training", "Formación", "التدريب", "トレーニング", "培训"),
    "nav.normes-certification": ("Normes & certification", "Standards & certification", "Normas y certificación", "المعايير والاعتماد", "規格と認証", "标准与认证"),
    "nav.standards-hub": ("Standards Hub", "Standards Hub", "Standards Hub", "مركز المعايير", "規格ハブ", "标准中心"),
    "nav.conformite-ia-ai-act": ("Conformité — IA (AI Act)", "Compliance — AI (AI Act)", "Cumplimiento — IA (AI Act)", "الامتثال — الذكاء الاصطناعي (AI Act)", "コンプライアンス — AI（AI法）", "合规 — AI（AI 法案）"),
    "nav.qms": ("QMS", "QMS", "QMS", "نظام إدارة الجودة", "QMS", "QMS"),
    "nav.conformite": ("Conformité", "Compliance", "Cumplimiento", "الامتثال", "コンプライアンス", "合规"),
    "nav.incidents": ("Incidents", "Incidents", "Incidentes", "الحوادث", "インシデント", "事件"),
    "nav.eudb": ("EUDB", "EUDB", "EUDB", "EUDB", "EUDB", "EUDB"),
    "nav.fria": ("FRIA", "FRIA", "FRIA", "FRIA", "FRIA", "FRIA"),
    "nav.pmm": ("PMM", "PMM", "PMM", "PMM", "PMM", "PMM"),
    "nav.conformite-donnees-rgpd": ("Conformité — Données (RGPD)", "Compliance — Data (GDPR)", "Cumplimiento — Datos (RGPD)", "الامتثال — البيانات (GDPR)", "コンプライアンス — データ（GDPR）", "合规 — 数据（GDPR）"),
    "nav.registre-ropa": ("Registre (RoPA)", "Register (RoPA)", "Registro (RoPA)", "السجل (RoPA)", "処理記録（RoPA）", "记录（RoPA）"),
    "nav.consentements": ("Consentements", "Consents", "Consentimientos", "الموافقات", "同意管理", "同意管理"),
    "nav.demandes-dsar": ("Demandes (DSAR)", "Requests (DSAR)", "Solicitudes (DSAR)", "الطلبات (DSAR)", "請求（DSAR）", "请求（DSAR）"),
    "nav.mentions": ("Mentions", "Notices", "Avisos", "الإشعارات", "プライバシー通知", "隐私声明"),
    "nav.dpia": ("DPIA", "DPIA", "EIPD", "تقييم الأثر (DPIA)", "DPIA", "DPIA"),
    "nav.dpo": ("DPO", "DPO", "DPD", "مسؤول حماية البيانات", "DPO", "DPO"),
    "nav.retention": ("Rétention", "Retention", "Retención", "الاحتفاظ", "保持期間", "保留"),
    "nav.transferts": ("Transferts", "Transfers", "Transferencias", "عمليات النقل", "越境移転", "数据传输"),
    "nav.sous-traitants-dpa": ("Sous-traitants (DPA)", "Processors (DPA)", "Encargados (DPA)", "المعالجون (DPA)", "処理者（DPA）", "处理方（DPA）"),
    "nav.violations": ("Violations", "Breaches", "Brechas", "الانتهاكات", "侵害", "数据泄露"),
    "nav.decisions-auto": ("Décisions auto.", "Automated decisions", "Decisiones autom.", "القرارات الآلية", "自動意思決定", "自动化决策"),
    "nav.conformite-cyber-nis-2": ("Conformité — Cyber (NIS 2)", "Compliance — Cyber (NIS 2)", "Cumplimiento — Ciber (NIS 2)", "الامتثال — السيبراني (NIS 2)", "コンプライアンス — サイバー（NIS 2）", "合规 — 网络（NIS 2）"),
    "nav.mesures": ("Mesures", "Measures", "Medidas", "التدابير", "対策", "措施"),
    "nav.incidents-cyber": ("Incidents cyber", "Cyber incidents", "Incidentes ciber", "الحوادث السيبرانية", "サイバーインシデント", "网络事件"),
    "nav.integrations": ("Intégrations", "Integrations", "Integraciones", "التكاملات", "連携", "集成"),
    "nav.itsm": ("ITSM", "ITSM", "ITSM", "ITSM", "ITSM", "ITSM"),
}

HEADER = (
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    '<!-- Généré par scripts/gen-i18n-xlf.py — NE PAS éditer à la main :\n'
    '     modifier la table TRANSLATIONS puis regénérer. -->\n'
    '<xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">\n'
    '  <file source-language="fr" target-language="{lang}" datatype="plaintext" original="ng2.template">\n'
    "    <body>\n"
)
FOOTER = "    </body>\n  </file>\n</xliff>\n"


def esc(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def main() -> None:
    out_dir = os.path.join(os.path.dirname(__file__), "..", "src", "locale")
    os.makedirs(out_dir, exist_ok=True)
    for idx, lang in enumerate(LANGS, start=1):
        path = os.path.join(out_dir, f"messages.{lang}.xlf")
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(HEADER.format(lang=lang))
            for unit_id, row in TRANSLATIONS.items():
                source, target = row[0], row[idx]
                f.write(f'      <trans-unit id="{unit_id}" datatype="html">\n')
                f.write(f"        <source>{esc(source)}</source>\n")
                f.write(f'        <target state="translated">{esc(target)}</target>\n')
                f.write("      </trans-unit>\n")
            f.write(FOOTER)
        print(f"OK {path} ({len(TRANSLATIONS)} unités)")


if __name__ == "__main__":
    main()
