import { DocumentFolder } from './documents.types';

/** Un dossier tel que l'arbre l'affiche : à plat, avec sa profondeur. */
export interface FolderRow {
  folder: DocumentFolder;
  depth: number;
  hasChildren: boolean;
}

/** L'arbre mis à plat dans l'ordre d'affichage : chaque dossier suivi de ses enfants. */
export function buildFolderRows(folders: DocumentFolder[]): FolderRow[] {
  const byParent = new Map<string | null, DocumentFolder[]>();
  for (const f of folders) {
    const key = f.parentId ?? null;
    byParent.set(key, [...(byParent.get(key) ?? []), f]);
  }
  for (const list of byParent.values()) {
    list.sort((a, b) => a.position - b.position || a.name.localeCompare(b.name));
  }
  const rows: FolderRow[] = [];
  const seen = new Set<string>();
  const walk = (parent: string | null, depth: number) => {
    for (const f of byParent.get(parent) ?? []) {
      if (seen.has(f.id)) continue;
      seen.add(f.id);
      rows.push({ folder: f, depth, hasChildren: (byParent.get(f.id) ?? []).length > 0 });
      walk(f.id, depth + 1);
    }
  };
  walk(null, 0);
  return rows;
}
