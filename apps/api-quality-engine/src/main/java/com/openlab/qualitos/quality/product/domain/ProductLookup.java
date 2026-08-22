package com.openlab.qualitos.quality.product.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les deux seules lectures dont le moteur de propositions de révision a besoin.
 *
 * <p>Il pourrait dépendre de {@link ProductRepository}, mais il hériterait alors
 * du droit d'écrire et de supprimer des produits — un droit qu'il n'exerce jamais
 * et qu'un futur contributeur pourrait croire légitime d'utiliser.
 */
public interface ProductLookup {

    Optional<Product> findById(UUID id);

    List<ProductOperation> operationsOf(UUID productId);
}
