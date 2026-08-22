package com.openlab.qualitos.quality.controlplan.domain;

/**
 * Ce que la ligne surveille : une ENTRÉE du poste ou sa SORTIE.
 *
 * <p>À ne pas confondre avec {@link CharacteristicType}, qui dit si la
 * caractéristique porte sur le produit ou sur le procédé. Les deux axes se
 * croisent : la température d'un four est une entrée de procédé, le diamètre
 * d'un alésage une sortie produit.
 *
 * <p>Pourquoi cela mérite une colonne : contrôler une sortie constate un défaut
 * déjà fait, contrôler une entrée l'empêche. Un plan qui ne surveille que des
 * sorties trie, il ne maîtrise pas — et cela se voit d'un coup d'œil quand la
 * colonne existe.
 */
public enum InputOutput {
    INPUT,
    OUTPUT
}
