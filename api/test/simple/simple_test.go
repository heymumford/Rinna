package simple

import (
	"testing"
)

func TestSimple(t *testing.T) {
	// This is a simple test that always passes
	if 1+1 != 2 {
		t.Errorf("Expected 1+1 to be 2, got %d", 1+1)
	}
}
