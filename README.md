# PlayerKits2 - Optimized

# Update Log
- **Optimized Kit Lookup:** Replaced the old `ArrayList<Kit>` with a more efficient `HashMap<String, Kit>`. This change drastically improves the speed of kit retrieval, creation, and deletion by allowing constant-time (O(1)) operations.

- **Addition to API:** Added `KitPreClaimEvent` this event is cancleable with CancelReason.

- **More Optimizations:** : More optimizations were made to more classes. Async loading was aslo implemented for configuration and postConfigSetup.

- **Update Checker** : Now points to the release page here.
  
