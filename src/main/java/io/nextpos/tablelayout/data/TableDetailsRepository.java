package io.nextpos.tablelayout.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TableDetailsRepository extends JpaRepository<TableLayout.TableDetails, String> {
}
