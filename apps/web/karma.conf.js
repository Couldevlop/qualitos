// Karma configuration for QualitOS web frontend.
// Génère un coverage HTML + lcov dans coverage/qualitos-web.
module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: { jasmine: {}, clearContext: false },
    jasmineHtmlReporter: { suppressAll: true },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/qualitos-web'),
      subdir: '.',
      reporters: [{ type: 'html' }, { type: 'text-summary' }, { type: 'lcovonly' }],
      // Gate de couverture BLOQUANT (le front n'en avait aucun : la règle §14.4
      // n'était donc pas tenue de ce côté, contrairement au back qui a son gate
      // JaCoCo à 0.85/0.75 depuis toujours).
      //
      // Fonctionnement en CLIQUET : les seuils sont calés juste sous la couverture
      // réelle mesurée, ce qui interdit toute régression dès aujourd'hui. Ils sont
      // remontés à chaque lot de tests ajouté, jusqu'à la cible de 99 %.
      //
      //   Mesure du 2026-08-01 : statements 84,54 % · branches 76,95 %
      //                          lines 86,11 %      · functions 87,10 %
      //   (+19,6 pts après l'intégration de la vague 2 — 4 écrans (IoT, connecteurs,
      //    registre des systèmes d'IA, compléments Standards Hub) et 339 specs.)
      //   Cible §14.4 : 85 / 75 — ATTEINTE sur les branches, à 0,5 pt sur les
      //   statements. Cible projet : 99.
      //
      // `emitWarning: false` = la suite sort en échec si un seuil n'est pas atteint.
      check: {
        emitWarning: false,
        global: {
          statements: 84,
          lines: 86,
          branches: 76,
          functions: 87
        }
      }
    },
    reporters: ['progress', 'kjhtml'],
    // La suite est longue (700+ specs) et l'instrumentation de couverture ralentit
    // encore le navigateur : le défaut de 30 s de silence toléré le fait tomber en
    // DISCONNECTED au milieu du run (et Karma sort alors en succès, ce qui masque
    // le problème). 120 s laissent passer les specs les plus lourdes.
    browserNoActivityTimeout: 120000,
    browserDisconnectTimeout: 10000,
    browserDisconnectTolerance: 2,
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['Chrome'],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
      }
    },
    singleRun: false,
    restartOnFileChange: true
  });
};
