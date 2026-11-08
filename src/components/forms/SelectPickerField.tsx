import React, { useState } from 'react';
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAppTheme } from '@/hooks/useAppTheme';

export interface PickerOption {
  label: string;
  value: string;
  subtitle?: string;
}

export interface SelectPickerFieldProps {
  label: string;
  placeholder: string;
  selectedValue: string;
  options: readonly PickerOption[];
  onValueChange: (value: string) => void;
  error?: string;
  disabled?: boolean;
  leadingIcon?: keyof typeof Ionicons.glyphMap;
  testID?: string;
}

export function SelectPickerField({
  label,
  placeholder,
  selectedValue,
  options,
  onValueChange,
  error,
  disabled = false,
  leadingIcon,
  testID,
}: SelectPickerFieldProps) {
  const { colors, typography } = useAppTheme();
  const [modalVisible, setModalVisible] = useState(false);

  const selectedOption = options.find((opt) => opt.value === selectedValue);

  return (
    <View style={styles.fieldContainer}>
      <Text
        style={[
          typography.labelLarge,
          styles.label,
          { color: colors.onSurface },
        ]}
      >
        {label}
      </Text>

      <Pressable
        disabled={disabled}
        onPress={() => setModalVisible(true)}
        style={[
          styles.selectorButton,
          {
            backgroundColor: colors.inputBackground,
            borderColor: error ? colors.error : colors.border,
            opacity: disabled ? 0.55 : 1,
          },
        ]}
        testID={testID}
        accessibilityRole="button"
        accessibilityLabel={`${label}: ${selectedOption?.label ?? placeholder}`}
      >
        {leadingIcon ? (
          <Ionicons
            name={leadingIcon}
            size={20}
            color={error ? colors.error : colors.onSurfaceVariant}
            style={styles.leadingIcon}
          />
        ) : null}

        <Text
          style={[
            typography.bodyLarge,
            styles.valueText,
            {
              color: selectedOption
                ? colors.onSurface
                : colors.onSurfaceVariant,
            },
          ]}
          numberOfLines={1}
        >
          {selectedOption ? selectedOption.label : placeholder}
        </Text>

        <Ionicons
          name="chevron-down"
          size={18}
          color={colors.onSurfaceVariant}
        />
      </Pressable>

      {error ? (
        <Text
          style={[
            typography.bodyMedium,
            styles.errorText,
            { color: colors.error },
          ]}
        >
          {error}
        </Text>
      ) : null}

      <Modal
        visible={modalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setModalVisible(false)}
      >
        <Pressable
          style={styles.modalBackdrop}
          onPress={() => setModalVisible(false)}
        >
          <Pressable
            style={[
              styles.modalSheet,
              {
                backgroundColor: colors.surface,
                borderColor: colors.border,
              },
            ]}
            onPress={(e) => e.stopPropagation()}
          >
            <View
              style={[
                styles.modalHeader,
                { borderBottomColor: colors.border },
              ]}
            >
              <Text
                style={[typography.titleMedium, { color: colors.onSurface }]}
              >
                {label}
              </Text>
              <Pressable
                onPress={() => setModalVisible(false)}
                hitSlop={10}
                accessibilityRole="button"
                accessibilityLabel="Close selector"
              >
                <Ionicons name="close" size={22} color={colors.onSurface} />
              </Pressable>
            </View>

            <ScrollView style={styles.optionsList}>
              {options.map((option) => {
                const isSelected = option.value === selectedValue;
                return (
                  <Pressable
                    key={option.value}
                    onPress={() => {
                      onValueChange(option.value);
                      setModalVisible(false);
                    }}
                    style={[
                      styles.optionRow,
                      {
                        backgroundColor: isSelected
                          ? colors.primaryContainer
                          : 'transparent',
                        borderBottomColor: colors.border,
                      },
                    ]}
                  >
                    <View style={{ flex: 1 }}>
                      <Text
                        style={[
                          typography.bodyLarge,
                          {
                            color: isSelected
                              ? colors.onPrimaryContainer
                              : colors.onSurface,
                            fontWeight: isSelected ? '600' : '400',
                          },
                        ]}
                      >
                        {option.label}
                      </Text>
                      {option.subtitle ? (
                        <Text
                          style={[
                            typography.bodyMedium,
                            { color: colors.onSurfaceVariant },
                          ]}
                        >
                          {option.subtitle}
                        </Text>
                      ) : null}
                    </View>
                    {isSelected ? (
                      <Ionicons
                        name="checkmark-circle"
                        size={20}
                        color={colors.secondary}
                      />
                    ) : null}
                  </Pressable>
                );
              })}
            </ScrollView>
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  fieldContainer: {
    marginBottom: 14,
  },
  label: {
    marginBottom: 6,
  },
  selectorButton: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 50,
    borderRadius: 12,
    borderWidth: 1,
    paddingHorizontal: 14,
  },
  leadingIcon: {
    marginRight: 10,
  },
  valueText: {
    flex: 1,
  },
  errorText: {
    marginTop: 4,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(6, 16, 30, 0.6)',
    justifyContent: 'center',
    padding: 20,
  },
  modalSheet: {
    borderRadius: 16,
    borderWidth: 1,
    maxHeight: '75%',
    overflow: 'hidden',
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 18,
    paddingVertical: 14,
    borderBottomWidth: 1,
  },
  optionsList: {
    paddingHorizontal: 8,
    paddingVertical: 6,
  },
  optionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 12,
    borderRadius: 10,
    marginVertical: 2,
  },
});
