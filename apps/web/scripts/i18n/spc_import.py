# -*- coding: utf-8 -*-
"""Table i18n - import Excel de la grille de mesures SPC. id: (fr, en, es, ar, ja, zh)."""

TRANSLATIONS = {
    'spc.import.title': ('Import Excel', 'Excel import', 'Importar de Excel',
                         'استيراد من Excel', 'Excel 取り込み', 'Excel 导入'),

    'spc.import.download-template': (
        'Télécharger le modèle', 'Download the template', 'Descargar la plantilla',
        'تنزيل القالب', 'ひな形をダウンロード', '下载模板'),

    'spc.import.choose': ('Importer un classeur', 'Import a workbook', 'Importar un libro',
                          'استيراد مصنّف', 'ブックを取り込む', '导入工作簿'),

    'spc.import.reading': ('Lecture…', 'Reading…', 'Leyendo…', 'جارٍ القراءة…', '読み込み中…', '正在读取…'),

    'spc.import.hint': (
        "Une ligne par pièce, une colonne par sous-groupe. L'en-tête et la colonne des rangs sont ignorés ; rien n'est enregistré avant que vous n'enregistriez l'étude.",
        'One row per part, one column per subgroup. The header row and the rank column are ignored; nothing is saved until you save the study.',
        'Una fila por pieza, una columna por subgrupo. La fila de encabezado y la columna de orden se ignoran; nada se guarda hasta que guarde el estudio.',
        'سطر لكل قطعة، وعمود لكل مجموعة فرعية. يُتجاهل سطر العناوين وعمود الترتيب؛ ولا يُحفظ شيء قبل أن تحفظ الدراسة.',
        '1 部品につき 1 行、1 サブグループにつき 1 列。見出し行と順位列は無視されます。研究を保存するまで何も保存されません。',
        '每个零件一行，每个子组一列。表头行与序号列会被忽略；在您保存研究之前不会保存任何内容。'),

    'spc.import.warnings-title': (
        "Ce que la lecture a écarté", 'What the reading discarded', 'Lo que la lectura descartó',
        'ما استبعدته القراءة', '取り込みで除外されたもの', '读取时被忽略的内容'),

    'spc.import.wrong-extension': (
        'Attendu un classeur Excel .xlsx (ni .xls, ni .csv).',
        'An Excel .xlsx workbook is expected (neither .xls nor .csv).',
        'Se espera un libro de Excel .xlsx (ni .xls ni .csv).',
        'المطلوب مصنّف Excel بصيغة .xlsx (لا .xls ولا .csv).',
        'Excel の .xlsx ブックが必要です（.xls や .csv は不可）。',
        '需要 Excel .xlsx 工作簿（不支持 .xls 或 .csv）。'),

    'spc.import.failed': (
        "Le classeur n'a pas pu être lu.", 'The workbook could not be read.',
        'No se pudo leer el libro.', 'تعذّر قراءة المصنّف.',
        'ブックを読み込めませんでした。', '无法读取该工作簿。'),

    'spc.import.template-failed': (
        "Le modèle n'a pas pu être téléchargé.", 'The template could not be downloaded.',
        'No se pudo descargar la plantilla.', 'تعذّر تنزيل القالب.',
        'ひな形をダウンロードできませんでした。', '无法下载模板。'),

    'spc.import.confirm-title': (
        'Remplacer les mesures saisies ?', 'Replace the measurements entered?',
        '¿Reemplazar las mediciones introducidas?', 'استبدال القياسات المُدخلة؟',
        '入力済みの測定値を置き換えますか？', '要替换已输入的测量值吗？'),

    'spc.import.confirm-message': (
        "Le classeur remplace toutes les mesures de la grille. L'identification de la pièce et les limites ne changent pas, et rien n'est enregistré tant que vous n'enregistrez pas l'étude.",
        'The workbook replaces every measurement in the grid. The part identification and the limits are unchanged, and nothing is saved until you save the study.',
        'El libro reemplaza todas las mediciones de la cuadrícula. La identificación de la pieza y los límites no cambian, y nada se guarda hasta que guarde el estudio.',
        'يستبدل المصنّف جميع القياسات في الشبكة. ولا يتغير تعريف القطعة ولا الحدود، ولا يُحفظ شيء قبل أن تحفظ الدراسة.',
        'ブックはグリッド内のすべての測定値を置き換えます。部品の識別情報と規格限界は変わらず、研究を保存するまで何も保存されません。',
        '该工作簿将替换网格中的全部测量值。零件标识与规格界限保持不变，在您保存研究之前不会保存任何内容。'),

    'spc.import.confirm': (
        'Importer le classeur', 'Import the workbook', 'Importar el libro',
        'استيراد المصنّف', 'ブックを取り込む', '导入工作簿'),
}
