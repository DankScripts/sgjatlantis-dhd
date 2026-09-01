def scaled($value; $center; $factor):
  $center + (($value - $center) * $factor);

def scale_origin($center_x; $center_z; $factor):
  if (.origin? | type) == "array" then
    .origin[0] = scaled(.origin[0]; $center_x; $factor)
    | .origin[2] = scaled(.origin[2]; $center_z; $factor)
  else . end;

def scale_element($center_x; $center_z; $factor):
  .from[0] = scaled(.from[0]; $center_x; $factor)
  | .from[2] = scaled(.from[2]; $center_z; $factor)
  | .to[0] = scaled(.to[0]; $center_x; $factor)
  | .to[2] = scaled(.to[2]; $center_z; $factor)
  | scale_origin($center_x; $center_z; $factor)
  | if ((.rotation? | type) == "object"
        and (.rotation.origin? | type) == "array") then
      .rotation.origin[0] = scaled(.rotation.origin[0]; $center_x; $factor)
      | .rotation.origin[2] = scaled(.rotation.origin[2]; $center_z; $factor)
    else . end;

def is_dialer_element($index):
  (($index >= 8 and $index <= 12) or ($index >= 23 and $index <= 278));

def is_dialer_group:
  (.name? == "plate")
  or ((.name? // "") | test("^city_button_[0-9]+$"))
  or ((.name? // "") | test("^center_crystal_[0-9]+$"));

def scale_groups($center_x; $center_z; $factor):
  walk(
    if type == "object" and is_dialer_group then
      scale_origin($center_x; $center_z; $factor)
    else . end
  );

.elements = (
    .elements
    | to_entries
    | map(
        if is_dialer_element(.key) then
          .value |= scale_element(8.8; 6.7; 1.25)
        else . end
      )
    | map(.value)
  )
| scale_groups(8.8; 6.7; 1.25)
