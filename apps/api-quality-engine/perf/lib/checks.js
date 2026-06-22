// Assertions réutilisables sur les réponses de l'API.

import { check } from 'k6';

// Vérifie qu'une réponse de listing paginé (Spring Data Page<T>) est valide :
// status 200 + corps avec un tableau `content`.
export function checkPage(res, name) {
  return check(res, {
    [`${name}: status 200`]: (r) => r.status === 200,
    [`${name}: corps Page (content[])`]: (r) => {
      try {
        return Array.isArray(r.json('content'));
      } catch (_e) {
        return false;
      }
    },
  });
}

// Vérifie qu'une réponse renvoie un tableau JSON nu (List<T>) ou une Page.
export function checkListOrPage(res, name) {
  return check(res, {
    [`${name}: status 200`]: (r) => r.status === 200,
    [`${name}: corps JSON (array ou Page)`]: (r) => {
      try {
        const b = r.json();
        return Array.isArray(b) || Array.isArray(r.json('content'));
      } catch (_e) {
        return false;
      }
    },
  });
}
