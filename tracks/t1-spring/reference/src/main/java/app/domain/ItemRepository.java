package app.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, String> {

    List<Item> findBySkuIn(List<String> skus);
}
