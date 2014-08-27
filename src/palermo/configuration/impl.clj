(ns palermo.configuration.impl
  (require [clj-yaml.core :as yaml]))

(defn load-configuration-file
  "Loads a configuration expressed as a series of nested hashes into memory"
  [path]
  (let [path-to-load path]
    (load-file path-to-load)))

(defn configuration-to-yaml
  "Transforms a configuration composed of nested hashes into a YAML string"
  [configuration]
  (yaml/generate-string configuration :dumper-options {:flow-style :block}))
