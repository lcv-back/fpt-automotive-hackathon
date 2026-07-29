BINARY := viva-tools

.PHONY: build test vet run clean

build:
	go build -o $(BINARY) ./cmd/viva-tools

test:
	go test ./...

vet:
	go vet ./...

# Usage: make run ARGS="harness report --input testdata/sample_trace.log --out report.csv"
run: build
	./$(BINARY) $(ARGS)

clean:
	rm -f $(BINARY) $(BINARY).exe
