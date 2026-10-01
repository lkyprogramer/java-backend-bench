# Design

Controller -> InventoryService (@Transactional) -> Spring Data repositories. Controller methods are synchronized so
only one request mutates stock at a time. Expired reservations are released at the start of each service call.
