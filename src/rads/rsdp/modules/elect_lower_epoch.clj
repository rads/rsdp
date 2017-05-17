(ns rads.rsdp.modules.elect-lower-epoch
  "Algorithm 2.9: Elect Lower Epoch
  
  Module:
    - Name: EventualLeaderDetector, instance eld.

  Events:
    - Indication: ⟨ eld, Trust | p ⟩
      - Indicates that process p is trusted to be leader.

  Properties:
    - ELD1: Eventual accuracy
      - There is a time after which every correct process trusts some correct
        process.
    - ELD2: Eventual agreement
      - There is a time after which no two correct processes trust different
        correct processes.")
